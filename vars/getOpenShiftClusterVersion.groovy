// vars/getOpenShiftClusterVersion.groovy
String call(boolean fullVersion = false) {
    node() {
        def versionJson = sh(returnStdout: true, script: 'oc get --raw /version/openshift')

        if (fullVersion) {
            def gitVersion = versionJson =~ /"gitVersion": "v([\d\.]*)\+?"/
            return gitVersion[0][1]
        }

        def major = versionJson =~ /"major": "(\d+)"/
        def minor = versionJson =~ /"minor": "(\d+)\+?"/

        return "${major[0][1]}.${minor[0][1]}"
    }
}
