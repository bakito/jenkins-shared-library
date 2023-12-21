#!/usr/bin/groovy

import com.six_group.jenkins.OpenShiftUtils

def call() {
    def utils = new OpenShiftUtils()
    return utils.getNamespace()
}
