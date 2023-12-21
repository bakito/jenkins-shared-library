#!/usr/bin/groovy
package com.six_group.jenkins

import io.fabric8.kubernetes.api.model.EnvVar

/**
 * A convenience class for the podTemplate of the https://github.com/jenkinsci/kubernetes-plugin. <br/>
 * Additional fields can be set on the available member fields
 */
class PipelinePod implements Serializable {

    def steps
    String image

    boolean alwaysPullImage = true
    boolean ttyEnabled = true
    String command = ''
    String args = ''
    String pipelinePodName = "jenkins-pipeline-pod-${UUID.randomUUID()}"
    String pipelineContainerName = pipelinePodName
    def envVars = []
    def volumes = []
    String resourceLimitCpu = '1000m'
    String resourceLimitMemory = '4Gi'
    String resourceRequestCpu = '500m'
    String resourceRequestMemory = '2Gi'
    // deprecated do not use
    int timeoutSeconds = 0
    String workingDir = ''
    String serviceAccount = 'jenkins'
    String nodeSelector = ''
    List imagePullSecrets = []
    int idleMinutes
    String yaml = ''

    /**
     * Constructor
     * @param steps the pipeline instance, just use 'this' when calling form the pipeline
     * @param image the docker image to be used in this pod
     */
    PipelinePod(steps, String image) {
        this.steps = steps
        this.image = image
    }

    /**
     * Execute the pod
     * @param body the closure to be executed within the pod.
     */
    def execute(Closure body) {
        if (body) {
            EnvVar
            steps.podTemplate(
                    cloud: 'openshift',
                    name: pipelinePodName,
                    label: pipelinePodName,
                    volumes: volumes,
                    imagePullSecrets: imagePullSecrets,
                    serviceAccount: serviceAccount,
                    nodeSelector: nodeSelector,
                    idleMinutes: idleMinutes,
                    yaml: yaml,
                    containers: [
                            steps.containerTemplate(
                                    name: pipelineContainerName,
                                    image: image.trim(),
                                    ttyEnabled: ttyEnabled,
                                    alwaysPullImage: alwaysPullImage,
                                    command: command,
                                    args: args,
                                    resourceLimitCpu: resourceLimitCpu,
                                    resourceLimitMemory: resourceLimitMemory,
                                    resourceRequestCpu: resourceRequestCpu,
                                    resourceRequestMemory: resourceRequestMemory,
                                    envVars: envVars,
                                    workingDir: workingDir
                            )
                    ]) {
                steps.node(pipelinePodName) {
                    if ('jnlp' == pipelineContainerName) {
                        body.call()
                    } else {
                        steps.container(pipelineContainerName) {
                            body.call()
                        }
                    }
                }
            }
        }
    }

    /**
     * Sets the parameters used to run the pod template as single jnlp pod.
     * @param podName
     * @return
     */
    def asJnlpAgent() {
        this.pipelineContainerName = 'jnlp'
        this.args = '${computer.jnlpmac} ${computer.name}'
        this.workingDir = '/tmp'
    }

    /**
     * Add the label to enable host aliases feature.
     * With host aliases, you can connect with the real host-name of the destination,
     * rather than with the egress route host-name.
     * See: https://confluence.six-group.net/display/OPSHI/Host+Aliases
     */
    def enableEgressHostAliases() {
        this.yaml +=
        """
            metadata:
                labels:
                    egress.six-group.com/inject-hostaliases: 'true'
        """
    }
}
