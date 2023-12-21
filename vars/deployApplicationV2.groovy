#!/usr/bin/groovy

import com.six_group.jenkins.DeploymentUtils

def call(String changeNo, String namespace, String repo, String tag, String username, String password) {
    call(changeNo: changeNo, namespace: namespace, repo: repo, tag: tag, username: username, password: password)
}

def call(String changeNo, String namespace, String repo, String tag, String secretName = null) {
    call(changeNo: changeNo, namespace: namespace, repo: repo, tag: tag, secretName: secretName)
}

def call(Map config) {
    def utils = new DeploymentUtils(this, openshift)
    utils.init(config)

    utils.updateProject()
}