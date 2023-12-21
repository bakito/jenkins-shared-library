#!/usr/bin/groovy

import com.six_group.jenkins.DeploymentUtils

def call(String namespace, String repo, String tag, String username, String password, boolean isUow2 = true) {
    call(namespace: namespace, repo: repo, tag: tag, username: username, password: password, isUow2: isUow2)
}

def call(String namespace, String repo, String tag, String secretName, boolean isUow2 = true) {
    call(namespace: namespace, repo: repo, tag: tag, secretName: secretName, isUow2: isUow2)
}

def call(Map config) {
    def utils = new DeploymentUtils(this, openshift)

    config.skipDeploy = true
    config.skipUndeploy = true
    if (config.containsKey("isUow2") && !config.isUow2) {
        config.uowFile = "uow.json"
    }
    utils.init(config)

    utils.updateProject()
}