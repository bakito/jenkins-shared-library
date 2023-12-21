#!/usr/bin/groovy

import com.six_group.jenkins.OpenShiftUtils

def call(String secretName) {
    def utils = new OpenShiftUtils()
    return "${utils.getNamespace()}-${secretName}"
}
