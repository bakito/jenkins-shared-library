// vars/getDockerToken.groovy
def call(String login = "serviceaccount") {
    node('master') {
        // Read the auth token from the file defined in the env variable AUTH_TOKEN
        String token = sh(returnStdout: true, script: 'cat ${AUTH_TOKEN}').trim()

        String prefix
        if (login) {
            prefix = "${login}:"
        } else {
            prefix = ''
        }
        return prefix + token
    }
}
