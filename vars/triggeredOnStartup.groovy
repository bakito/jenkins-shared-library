// vars/triggeredOnStartup.groovy
def call(boolean setAborted = true, String message = "triggered by SIX jenkins autostart hook") {


    if (env.SIX_JENKINS_STARTUP_TRIGGERED == "true") {
        // abort the build when automatically triggered by deployment hook
        if (setAborted) {
            currentBuild.result = 'ABORTED'
            addInfoBadge id: 'SIX_JENKINS_STARTUP_TRIGGERED', text: message
        }
        echo message
        return true
    }

    return false
}
