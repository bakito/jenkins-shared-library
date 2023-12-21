#!/usr/bin/groovy
import com.six_group.jenkins.OpenShiftUtils

def call(Map config, Closure body) {
    def utils = new OpenShiftUtils()
    utils.connectToOpenshiftProject(this, config.cluster, config.project, config.credentialsId, body)
}