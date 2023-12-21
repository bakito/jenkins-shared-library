// vars/getInitialJobContext.groovy
def call(boolean withVersion = true) {
    def jobContext = [:]

    node() {
        if (withVersion) {
            // generate version number
            jobContext.currentBuildVersion = sh(returnStdout: true, script: 'date +%Y%m%d%H%M%S  -u').trim()
        }
    }
    return jobContext
}
