// vars/defaultProperties.groovy
def call(Integer numberToKeep = 5, String cron = null) {
    call(numberToKeep: numberToKeep, cronPollSCM: cron)
}


def call(Map config) {
    def list = []
    def triggers = []

    String message = "Applying build properties with ..."
    if (!config.containsKey("numberToKeep")) {
        config.numberToKeep = 5
    }
    message += "\n - ${config.numberToKeep} build number(s) to keep"
    list << buildDiscarder(logRotator(numToKeepStr: "${config.numberToKeep}"))

    if (config.cronPollSCM != null) {
        message += "\n - SCM poll cron trigger '${config.cronPollSCM}'"
        triggers << pollSCM(config.cronPollSCM)
    }

    if (config.cronTrigger != null) {
        message += "\n - cron trigger '${config.cronTrigger}'"
        triggers << cron(config.cronTrigger)
    }
    list << pipelineTriggers(triggers)
    echo message
    properties(list)
}