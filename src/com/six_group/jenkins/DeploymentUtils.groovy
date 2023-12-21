#!/usr/bin/groovy
package com.six_group.jenkins

import groovy.json.JsonOutput
import groovy.json.JsonSlurperClassic
import groovy.text.SimpleTemplateEngine
import org.yaml.snakeyaml.Yaml

class DeploymentUtils {
    static final String NAME_NOT_AVAILABLE = "N/A"
    static final String NAME_LIST = "#LIST#"

    private def pipeline
    private def openshift
    // the directory the repo is checkout out to
    private def deploymentWorkDir = null
    // the root directory for uow files
    private def uowRootDir = ''
    private def uowSubPath = 'deployment'
    private def uowFile = 'uowV2.json'
    private def skipDeploy = false
    private def skipUndeploy = false
    private def failOnValidationError = false
    private def failV1ToV2ConversionError = false
    private def enabledDeployments = []
    private def rolloutTimeoutMinutes = 10
    private def generateHelmChart = true
    private def gitCloneDepth = 2

    private def username
    private def password
    private def secretName

    private def changeNo
    private def namespace
    private def repo
    private def tag

    private Map namesByPath = [:]
    private List errors = []
    private List warnings = []

    DeploymentUtils(pipeline, openshift) {
        this.pipeline = pipeline
        this.openshift = openshift
    }

    def init(Map config) {
        copyProperty(config, "uowSubPath")
        copyProperty(config, "uowRootDir")
        copyProperty(config, "uowFile")
        copyProperty(config, "skipDeploy")
        copyProperty(config, "skipUndeploy")
        copyProperty(config, "failOnValidationError")
        copyProperty(config, "failV1ToV2ConversionError")
        copyProperty(config, "enabledDeployments")
        copyProperty(config, "rolloutTimeoutMinutes")
        copyProperty(config, "username")
        copyProperty(config, "password")
        copyProperty(config, "secretName")
        copyProperty(config, "changeNo")
        copyProperty(config, "namespace")
        copyProperty(config, "repo")
        copyProperty(config, "tag")
        copyProperty(config, "generateHelmChart")
        copyProperty(config, "gitCloneDepth")
    }

    private copyProperty(Map config, String key) {
        if (config.containsKey(key)) {
            this[key] = config[key]
        }
    }

    def updateProject() {
        if (this.username != null && this.password != null) {
            updateProjectBasicAuth(this.changeNo, this.namespace, this.repo, this.tag, this.username, this.password)
        } else {
            updateProjectSecret(this.changeNo, this.namespace, this.repo, this.tag, this.secretName)
        }
    }

    private updateProjectBasicAuth(String changeNo, String namespace, String repo, String tag, String username, String password) {
        try {
            def stage = defineStage(namespace)

            // check the change no (only for prod)
            if (namespace != "six-baseimages-unittest" && stage == "prod" && !validateChangeNo(changeNo)) {
                this.error("Invalid ChangeNo provided: ${changeNo}")
            }

            // check the username id (only for prod)
            if (namespace != "six-baseimages-unittest" && stage == "prod" && !validateUser(username)) {
                this.error("Invalid User provided: ${username}")
            }

            // checkout unit of work
            checkout(repo, tag, username, password)

            // check the git tag (only for prod)
            if (namespace != "six-baseimages-unittest" && stage == "prod" && !validateTag(tag)) {
                this.error("Invalid Tag provided: ${tag}")
            }

            // start openshift update
            updateOpenshift(namespace, stage)
        } finally {
            if (deploymentWorkDir != null) {
                pipeline.sh "rm -rf ${deploymentWorkDir}"
            }
        }
    }

    private updateProjectSecret(String changeNo, String namespace, String repo, String tag, String secretName) {
        try {
            def stage = defineStage(namespace)
            def secret = defineSecret(secretName)

            // checkout unit of work
            checkout(repo, tag, secret)

            // start openshift update
            updateOpenshift(namespace, stage)
        } finally {
            if (deploymentWorkDir != null) {
                pipeline.sh "rm -rf ${deploymentWorkDir}"
            }
        }
    }

    def updateOpenshift(String namespace, String stage) {

        if (isV1()) {
            this.warning("deployApplication is deprecated, please use deployApplicationV2")
        }

        pipeline.echo "Read ${getUowPath()}/${uowFile}"
        def json = pipeline.readJSON file: "${getUowPath()}/${uowFile}"

        pipeline.echo "Read ${getUowPath()}/attributes-${stage}.properties"
        def properties = pipeline.readProperties file: "${getUowPath()}/attributes-${stage}.properties"

        pipeline.echo "Update project ${namespace}"
        openshift.withCluster() {
            openshift.withProject(namespace) {
                if (!skipUndeploy) {
                    doUndeployment(json)
                } else {
                    pipeline.echo "Skipping Undeployment"
                }
                if (!skipDeploy) {
                    doDeployment(json, properties)
                } else {
                    pipeline.echo "Skipping Deployment"
                }
            }
        }

        convertToV2(json)
        if (generateHelmChart) {
            generateHelm(json, properties)
        }
        reportResults()
    }

    def doDeployment(Map uow, Map properties) {
        pipeline.echo ""
        pipeline.echo "Do deployments..."
        pipeline.echo ""

        if (uow.deployments == null) {
            return
        }

        uow.deployments.each { deployment ->
            def name = deployment.name
            if (enabledDeployments.isEmpty() || enabledDeployments.contains(name)) {
                pipeline.echo "Deployment of ${name}"
                pipeline.echo "---------------------------------------"

                checkedDeploy(deployment.deploymentconfigs, "DeploymentConfig", properties)

                // fallback for V1
                if (deployment.deploymentconfig != null && !(deployment.deploymentconfig instanceof List)) {
                    checkedDeploy([deployment.deploymentconfig], "DeploymentConfig", properties)
                }

                checkedDeploy(deployment.services, "Service", properties)
                checkedDeploy(deployment.routes, "Route", properties)
                checkedDeploy(deployment.configmaps, "ConfigMap", properties)
                checkedDeploy(deployment.persistentvolumes, "PersistentVolumeClaim", properties)
                checkedDeploy(deployment.imagestreams, "ImageStream", properties)
                checkedDeploy(deployment.jobs, "Job", properties)
                checkedDeploy(deployment.cronjobs, "CronJob", properties)

                if (deployment.deploymentconfigs != null) {
                    deployment.deploymentconfigs.each { deploymentconfig ->
                        def isRollout = true
                        if (deploymentconfig.rollout != null) {
                            isRollout = deploymentconfig.rollout
                        }
                        if (isRollout) {
                            doRollout(deploymentconfig.path)
                        }
                    }
                }

                // fallback for V1
                if (deployment.deploymentconfig != null && !(deployment.deploymentconfig instanceof List)) {
                    doRollout(deployment.deploymentconfig.path)
                }
            } else {
                pipeline.echo "Skipping Deployment of ${name}"
                pipeline.echo "---------------------------------------"
            }
        }
    }

    def checkedDeploy(List items, String kind, Map properties) {
        if (items != null) {
            items.each { item ->
                deploy(item.name, kind, item.path, properties)
            }
        }
    }

    def doRollout(String path) {
        def name = namesByPath[path]
        pipeline.echo ""
        pipeline.echo "Do rollout of DeploymentConfig ${name}..."
        pipeline.echo ""

        def dc = openshift.selector("DeploymentConfig", name)
        if (dc.exists()) {
            dc.rollout().latest()
            pipeline.timeout(rolloutTimeoutMinutes) {
                dc.rollout().status()
            }
        }
    }

    def doUndeployment(Map uow) {
        pipeline.echo ""
        pipeline.echo "Do undeployments..."
        pipeline.echo ""

        if (uow.undeployments == null) {
            return
        }

        uow.undeployments.each { undeployment ->
            def name = undeployment.name

            if (enabledDeployments.isEmpty() || enabledDeployments.contains(name)) {

                pipeline.echo "Undeployment of ${name}"
                pipeline.echo "---------------------------------------"

                checkedUndeploy(undeployment.deploymentconfigs, "DeploymentConfig")

                // fallback for V1
                if (undeployment.deploymentconfig != null) {
                    checkedUndeploy([undeployment.deploymentconfig], "DeploymentConfig")
                }

                checkedUndeploy(undeployment.services, "Service")
                checkedUndeploy(undeployment.routes, "Route")
                checkedUndeploy(undeployment.configmaps, "ConfigMap")
                checkedUndeploy(undeployment.persistentvolumes, "PersistentVolumeClaim")
                checkedUndeploy(undeployment.imagestreams, "ImageStream")
                checkedUndeploy(undeployment.jobs, "Job")
                checkedUndeploy(undeployment.cronjobs, "CronJob")
            } else {
                pipeline.echo "Skipping Undeployment of ${name}"
                pipeline.echo "---------------------------------------"
            }
        }
    }

    def checkedUndeploy(List items, String kind) {
        if (items != null) {
            items.each { item ->
                undeploy(item.name, kind)
            }
        }
    }

    def deploy(String name, String kind, String path, Map properties) {

        pipeline.echo "Prepare ${kind} ${path}"
        def fileContent = pipeline.readFile file: "${deploymentWorkDir}/${uowRootDir}/${path}"

        fileContent = replaceProperties('@@', fileContent, properties)

        def correctName = checkNameAndKind(path, fileContent, name, kind)
        if (correctName == null || correctName.isEmpty()) {
            error("The name for ${kind} '${path}' could not be evaluated.")
        }

        namesByPath[path] = correctName

        if (correctName == NAME_LIST) {
            this.warning("Deploy List of ${kind} from ${path}. This will be removed in later versions. Please use a single file for each ${kind} component.")
            openshift.apply(fileContent)
        } else if (openshift.selector(kind, correctName).exists()) {
            pipeline.echo "Deploy (replace) ${kind} ${correctName} from ${path}"
            openshift.apply(fileContent)
        } else {
            pipeline.echo "Deploy (create) ${kind} ${correctName} from ${path}"
            openshift.create(fileContent)
        }
    }

    private String replaceProperties(String marker, String content, Map properties) {
        def replaced = content
        properties.each { property ->
            def token = "${marker}${property.key}${marker}"
            def value = property.value

            if (replaced.contains(token)) {
                pipeline.echo "Replacing '${token}' with '${value}'"
                replaced = replaced.replaceAll(token, value)
            }
        }
        return replaced
    }

    def undeploy(String name, String type) {
        if (openshift.selector(type, name).exists()) {
            pipeline.echo "Undeploy ${type} ${name}"
            openshift.delete("${type}/${name}")
        }
    }

    String defineStage(String namespace) {
        def stage = namespace.substring(namespace.lastIndexOf("-") + 1)

        //special for blueprint project
        if (stage == "unittest") {
            stage = "dev"
        }
        // fix for manually created projects
        if (stage == "test") {
            stage = "tst"
        }

        if (!["dev", "tst", "int", "qa"].contains(stage)) {
            stage = "prod"
        }

        return stage;
    }

    String defineSecret(String secret) {
        return secret ? secret : "bitbucket-secret"
    }

    String checkout(String repo, String tag, String user, String pass) {
        def encodedPass = encodePassword(pass)
        pipeline.wrap([$class: 'MaskPasswordsBuildWrapper', varPasswordPairs: [[password: pass, var: 'SECRET'], [password: encodedPass, var: 'SECRET_ENC']]]) {

            if (deploymentWorkDir == null) {
                newDeploymentWorkDir()
            }

            pipeline.sh "rm -rf ${deploymentWorkDir}"

            pipeline.sh('#!/bin/sh -e\n' + "git clone --single-branch --depth ${gitCloneDepth} --branch ${tag} https://${user}:${encodedPass}@stash.six-group.net/scm${correctRepo(repo)} ${deploymentWorkDir}")
        }
    }

    String checkout(String repo, String tag, String secret) {
        if (deploymentWorkDir == null) {
            newDeploymentWorkDir()
        }
        pipeline.sh "rm -rf ${deploymentWorkDir}"

        pipeline.sshagent([pipeline.getCredentialsWithNamespace(secret)]) {
            if (repo.startsWith("ssh://")) {
                pipeline.sh "git clone --single-branch --depth ${gitCloneDepth} --branch ${tag} ${repo} ${deploymentWorkDir}"
            } else {
                pipeline.sh "git clone --single-branch --depth ${gitCloneDepth} --branch ${tag} ssh://git@stash.six-group.net:22${correctRepo(repo)} ${deploymentWorkDir}"
            }
        }
    }

    private String correctRepo(String repo) {
        if (repo.contains("://")) {
            // assuming we have a pull qualified path
            return repo
        }
        return repo.startsWith('/') ? repo : "/${repo}"
    }

    private void newDeploymentWorkDir() {
        deploymentWorkDir = "${pipeline.pwd()}/" + UUID.randomUUID().toString()
    }

    boolean validateChangeNo(String changeNo) {
        // TODO: implement
        return true
    }

    boolean validateUser(String user) {
        return user.startsWith("tk") || user.startsWith("tx")
    }

    boolean validateTag(String tag) {
        pipeline.dir("${deploymentWorkDir}") {
            return pipeline.sh(script: "git show-ref --verify refs/tags/${tag}", returnStatus: true) == 0
        }
    }

    static boolean validateNonProd(String stage) {
        return stage != "prod"
    }

    static String encodePassword(String password) {
        return URLEncoder.encode(password, "UTF-8")
    }

    def checkNameAndKind(String path, String fileContent, String name, String kind) {
        if (path != null && !path.isEmpty()) {
            def nak = evaluateNameAndKind("${deploymentWorkDir}/${uowRootDir}/${path}", fileContent)
            if (name != null && !name.isEmpty() && nak.name != NAME_NOT_AVAILABLE && nak.name != name) {
                def message = "The provided name '${name}' does not match the name '${nak.name}' of the resource defined in '${path}'"
                if (failOnValidationError) {
                    this.error(message)
                } else {
                    this.warning(message)
                }
            }

            if (nak.name != NAME_NOT_AVAILABLE && nak.kind.toLowerCase() != kind.toLowerCase()) {
                def message
                if (nak.list) {
                    message = "The file '${path}' contains a list, this is not supported by uow please provide single files if kind '${kind}'."
                } else {
                    message = "The resource kind '${nak.kind}' defined in '${path}' does not match the ouw kind '${kind}'"
                }
                if (failOnValidationError) {
                    this.error(message)
                } else {
                    this.warning(message)
                }
            }

            if (nak.list) {
                return NAME_LIST
            }
            if (nak.name != NAME_NOT_AVAILABLE) {
                return nak.name
            }
        }

        return name
    }

    def evaluateNameAndKind(String path, String fileContent) {
        try {
            Object element = readElement(path, fileContent)

            boolean isList = false
            if (element.kind.toLowerCase() == "list") {
                isList = true

                String lastKind = NAME_NOT_AVAILABLE
                element.items.each { item ->
                    if (lastKind != NAME_NOT_AVAILABLE) {
                        if (lastKind != item.kind) {
                            error("The file ${path} conatins a list of items of different kinds. This is not supported. Pliease split them into different files of the corresponding types.")
                        }
                    }
                    lastKind = item.kind
                }
            }

            return [name: element.metadata.name, kind: element.kind, list: isList]
        } catch (e) {
            pipeline.echo("ERROR: Could not evaluate .metadata.name of element in '" + path + " ' (${e.message}")
            return [name: NAME_NOT_AVAILABLE, kind: "", list: false]
        }
    }

    private Object readElement(String path, String fileContent) {

        if (path.endsWith(".yaml") || path.endsWith(".yml")) {
            return Yaml.newInstance().load(fileContent)

        } else if (path.endsWith(".json")) {
            return new JsonSlurperClassic().parseText(fileContent)
        }
        return null
    }

    def error(String message) {
        pipeline.echo("ERROR: ${message}")
        pipeline.error("ERROR: ${message}")
        this.errors.add(message)
    }

    def warning(String message) {
        pipeline.echo("WARNING: ${message}")
        this.warnings.add(message)
    }

    void convertToV2(json) {
        if (isV1()) {
            try {
                pipeline.echo "Converting uow.json to V2 please see the attached file."

                def newJson = [:]
                newJson.undeployments = copyDeployments(json.undeployments, false)
                newJson.deployments = copyDeployments(json.deployments, true)

                def pretty = JsonOutput.prettyPrint(JsonOutput.toJson(newJson))

                pipeline.writeFile(text: pretty, file: "uow2.converted.json", encoding: 'UTF-8')
                pipeline.archiveArtifacts artifacts: 'uow2.converted.json'
                pipeline.removeBadges(id: 'DeploymentUtils.uow2.converted.json')
                pipeline.addInfoBadge id: 'DeploymentUtils.uow2.converted.json',
                        text: 'Please find a converted uow2.json file here',
                        link: "${pipeline.env.BUILD_URL}artifact/uow2.converted.json/*view*/"
            } catch (e) {
                if (!failV1ToV2ConversionError) {
                    throw e
                }
            }
        }
    }

    private static List copyDeployments(List v1dep, boolean rollout) {
        List v2dep = []
        if (v1dep != null) {
            v1dep.each { deployment ->
                def nd = [:]
                deployment.each { prop, val ->
                    if (prop == 'deploymentconfig' && !(val instanceof List)) {
                        def dc = []
                        dc.add(val)
                        if (rollout) {
                            val.rollout = true
                        }
                        nd.deploymentconfigs = dc
                    } else {
                        nd[prop] = val
                    }
                }
                nd.jobs = []
                nd.cronjobs = []
                v2dep.add(nd)
            }
        }
        return v2dep
    }

    private reportResults() {
        printSummary("Warnings", "warning.gif", warnings)
        printSummary("Errors", "error.gif", errors)
    }

    private printSummary(String type, String icon, List messages) {
        if (!messages.isEmpty()) {
            def summary = pipeline.createSummary(icon: icon, id: "DeploymentUtils.${type}")
            summary.appendText("<h1>SIX Deployment Utils - ${type} ${uowFile}</h1><ul>", false)
            messages.each { message ->
                summary.appendText("<li>${message}</li>", false)
            }
            summary.appendText("</ul>", false)
            pipeline.removeBadges(id: "DeploymentUtils.Summary")
            pipeline.addBadge icon: icon,
                    id: 'DeploymentUtils.Summary',
                    text: "Your deployment has ${type.toLowerCase()}",
                    link: "${pipeline.env.BUILD_URL}"
        }
    }

    private isV1() {
        return uowFile == "uow.json"
    }


    private String getUowPath() {
        return "${deploymentWorkDir}/${uowRootDir}/${uowSubPath}".replaceAll('//', '/')
    }

    private generateHelm(Map uow, Map properties) {
        if (uow.deployments == null) {
            return
        }
        try {
            uow.deployments.each { deployment ->
                def name = deployment.name
                pipeline.sh "rm -Rf ${deployment.name}"
                def allPropFiles = pipeline.sh(returnStdout: true, script: "find ${getUowPath()} -type f -name attributes-*.properties -printf \"%f \"").trim().split(' ')
                Map allProperties = new HashMap()
                allPropFiles.each { pf ->
                    File file = new File(pf)
                    String stage = file.name.replaceAll('attributes-', '').replaceAll('.properties', '')
                    allProperties[stage] =  pipeline.readProperties file: pf
                }

                pipeline.dir(deployment.name) {
                    pipeline.echo "Generate Helm Chart for deployment ${name}"
                    pipeline.echo "---------------------------------------"

                    String chart = new SimpleTemplateEngine().createTemplate(CHART_YAML).make(ChartName: deployment.name)
                    pipeline.writeFile file: 'Chart.yaml', text: chart
                    pipeline.writeFile file: 'README.txt', text: DISCLAIMER
                    pipeline.writeYaml file: 'values.yaml', data: ["name": deployment.name, "properties": properties]

                    allProperties.each { stage, p ->
                        pipeline.writeYaml file: "${stage}-values.yaml", data: ["properties": p]
                    }

                    pipeline.dir("templates") {
                        String helper = new SimpleTemplateEngine().createTemplate(HELPERS_TPL).make(ChartName: deployment.name)
                        pipeline.writeFile file: '_helpers.tpl', text: helper

                        def placeholder = new HashMap<>()
                        properties.each { k, v ->
                            placeholder[k] = '{{ .Values.properties.' + k + ' }}'
                        }

                        toHelmChart(deployment.deploymentconfigs, placeholder, deployment.name)
                        // fallback for V1
                        if (deployment.deploymentconfig != null) {
                            toHelmChart([deployment.deploymentconfig], placeholder, deployment.name)
                        }
                        toHelmChart(deployment.services, placeholder, deployment.name)
                        toHelmChart(deployment.routes, placeholder, deployment.name)
                        toHelmChart(deployment.configmaps, placeholder, deployment.name)
                        toHelmChart(deployment.persistentvolumes, placeholder, deployment.name)
                        toHelmChart(deployment.imagestreams, placeholder, deployment.name)
                        toHelmChart(deployment.jobs, placeholder, deployment.name)
                        toHelmChart(deployment.cronjobs, placeholder, deployment.name)
                    }
                }

                pipeline.sh "rm -f helm-chart-${deployment.name}.zip"
                pipeline.zip archive: true, dir: deployment.name, zipFile: "helm-chart-${deployment.name}.zip"
                pipeline.removeBadges(id: "helm-chart-${deployment.name}.zip")
                pipeline.addBadge icon: 'https://helm.sh/img/favicon-152.png',
                        id: "helm-chart-${deployment.name}.zip",
                        text: 'Please find a converted helm chart here',
                        link: "${pipeline.env.BUILD_URL}artifact/helm-chart-${deployment.name}.zip"
            }
        } catch (e) {
            StringWriter sw = new StringWriter()
            e.printStackTrace(new PrintWriter(sw))

            pipeline.echo "Error generating Helm Chart: \n${sw}"
        }
    }

    def toHelmChart(List items, Map properties, String deploymentName) {
        if (items != null) {
            items.each { item ->
                String fileContent = pipeline.readFile file: "${deploymentWorkDir}/${uowRootDir}/${item.path}"

                fileContent = fileContent.replaceAll('@@', '_@@_')

                HashMap element = readElement(item.path, fileContent)

                if (!"List".equals(element.kind)) {
                    // create metadata labels if missing
                    if (!element.containsKey("metadata")) {
                        element["metadata"] = new HashMap<>()
                    }
                    if (!element.metadata.containsKey("labels")) {
                        element.metadata["labels"] = new HashMap<>()
                    }
                    // remove app, will be added by helm template
                    element.metadata.labels.remove('app')

                    switch (element.kind) {
                        case "DeploymentConfig":
                            element.apiVersion = "apps.openshift.io/v1"
                            break
                        case "ImageStream":
                            element.apiVersion = "image.openshift.io/v1"
                            break
                        case "CronJob":
                            element.apiVersion = "batch/v1beta1"
                            break
                        case "Route":
                            element.apiVersion = "route.openshift.io/v1"
                            if (!element.containsKey("status")) {
                                element["status"] = new HashMap<>()
                            }
                            if (!element.status.containsKey("ingress")) {
                                element.status["ingress"] = new ArrayList<>()
                            }
                            if (!element.spec.containsKey("host")) {
                                element.spec["host"] = "todo.six-group.net"
                            }
                            break
                    }

                    File f = new File(item.path)
                    def fileName = f.getName()
                    fileName = fileName.lastIndexOf('.').with { it != -1 ? fileName[0..<it] : fileName }

                    def templateFileName = "${element.kind.toLowerCase()}-${element.metadata.name}.yaml"
                    pipeline.writeYaml file: templateFileName, data: element

                    // read again to add helm template values
                    String yamlFile = pipeline.readFile file: templateFileName
                    yamlFile = replaceProperties('_@@_', yamlFile, properties)
                    String[] yamlLines = yamlFile.split('\n')

                    String helmTemplate = ''
                    for (int i = 0; i < yamlLines.length; i++) {
                        switch (yamlLines[i]) {
                            case '  labels:':
                            case '  labels: {}':
                                helmTemplate += '  labels:\n'
                                DEFAULT_LABELS
                                String labels = new SimpleTemplateEngine().createTemplate(DEFAULT_LABELS).make(ChartName: deploymentName)
                                helmTemplate += labels
                                break
                            default:
                                helmTemplate += yamlLines[i].replaceAll(deploymentName, "{{ template \"${deploymentName}.name\" . }}") + '\n'
                        }
                    }
                    pipeline.writeFile file: templateFileName, text: helmTemplate
                }
            }
        }
    }

    private static final String CHART_YAML = '''name: ${ChartName}
version: 1.0.0
appVersion: 1.0.0
description: TODO
sources:
  - TODO
maintainers:
  - email: TODO
    name: TODO
'''
    private static final String HELPERS_TPL = '''{{/* vim: set filetype=mustache: */}}

{{/* Create chart name */}}
{{- define "${ChartName}.name" -}}
{{- printf "%s" .Values.name | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}


{{/* helm labels */}}
{{- define "${ChartName}.helm-labels" -}}
chart: {{ .Chart.Name }}-{{ .Chart.Version }}
component: {{ .Values.name }}
heritage: {{ .Release.Service }}
release: {{ .Release.Name }}
{{- end -}}'''

    private static final String DEFAULT_LABELS = '''    app: {{ template "${ChartName}.name" . }}
{{ include "${ChartName}.helm-labels" . | indent 4 }}
'''

    private static final String DISCLAIMER = '''This helm 3 chart was automatically converted from uow.
It is meant be base for you to start with helm 3, not as a final chart.
Please verify the chart before applying it.
Check all TODO's in the generated files.

Find more information about helm here: https://v3.helm.sh/
Download the helm binaries from artifactory: https://artifactory.six-group.net/artifactory/list/opensource-generic-release-local/helm/
'''
}
