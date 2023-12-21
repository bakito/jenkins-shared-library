#!/usr/bin/groovy
package com.six_group.jenkins

import com.cloudbees.groovy.cps.NonCPS
import hudson.scm.SCM
import jenkins.model.Jenkins
import io.fabric8.jenkins.openshiftsync.GlobalPluginConfiguration
import java.util.List

@NonCPS
String getNamespace() {
    return GlobalPluginConfiguration.get().namespace
}

@NonCPS
String getGitBranch() {
    try {
        return getScm().branches[0].name
    } catch (e) {
        echo "WARNING: Failed to to get the current git branch of the current job: ${e}"
        e.printStackTrace()
    }
}

@NonCPS
String getGitRepo() {
    try {
        return getScm().repositories[0].URIs[0].toString()
    } catch (e) {
        echo "WARNING: Failed to to get the current git repo of the current job: ${e}"
        e.printStackTrace()
    }
}

String getGitRevision(boolean shortRevision = true) {
    String shortOption = shortRevision ? '--short' : ''
    return sh(returnStdout: true, script: "git rev-parse ${shortOption} HEAD").trim()
}

String getGitComment() {
    return sh(returnStdout: true, script: "git log -1 --pretty=%B").trim()
}

boolean should(String key) {
    return getGitComment().toLowerCase().contains("[${key.toLowerCase()}]")
}

boolean shouldNot(String key) {
    return !should(key)
}


List<String> getCurrentGitTag() {
    return sh(returnStdout: true, script: "git tag -l --points-at HEAD").trim().split('\n').toList()
}

@NonCPS
private SCM getScm() {
    def activeInstance = Jenkins.getInstanceOrNull()
    def job = activeInstance.getItemByFullName(env.JOB_NAME)
    return job.definition.scm
}

def connectToOpenshiftProject(script, cluster, project, credentialsId, Closure body) {
    script.echo "Trying to connect to project ${project} in cluster ${cluster} with credentials ${credentialsId}"
    if (body) {
        script.openshift.withCluster(cluster) {
            script.openshift.withCredentials(credentialsId) {
                script.openshift.withProject(project) {
                    script.echo "Connected to project ${script.openshift.project()} in cluster ${script.openshift.cluster()}"
                    body.call()
                }
            }
        }
    }
}
