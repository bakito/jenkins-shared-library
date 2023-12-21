import org.jenkinsci.plugins.credentialsbinding.impl.CredentialNotFoundException

// vars/imageMgmtNode.groovy

def call(String artifactoryBasicOutCredentialsId = null, Closure body) {
    call([artifactoryBasicAuth: artifactoryBasicOutCredentialsId], body)
}

def call(Map config, Closure body) {

    if (!config.containsKey("artifactoryBasicAuth")) {
        config.artifactoryBasicAuth = getCredentialsWithNamespace('artifactory')
    }

    if (!config.containsKey("dockerToken")) {
        config.dockerToken = getDockerToken()
    }
    if (config.containsKey("ocp4")) {
        config.image = 'artifactory.six-group.net/sdbi/ose-jenkins-agent-skopeo:v4.8'
    } else {
        config.image = 'artifactory.six-group.net/sdbi/jenkins-slave-image-mgmt:latest'
    }

    // we need credentials for skopeo (copy images from openshift to artifactory) and for the artifactory promotion.
    echo "Using credentials '${config.artifactoryBasicAuth}' to access artifactory."
    try {
        withCredentials([usernameColonPassword(credentialsId: config.artifactoryBasicAuth, variable: 'SKOPEO_DEST_CREDENTIALS')]) {
            pipelinePod(config.image, { pp ->
                pp.asJnlpAgent()
                pp.timeoutSeconds = 300
                pp.envVars = [envVar(key: 'SKOPEO_SRC_CREDENTIALS', value: config.dockerToken),
                              envVar(key: 'SKOPEO_DEST_CREDENTIALS', value: env.SKOPEO_DEST_CREDENTIALS),
                              envVar(key: 'ARTIFACTORY_BASIC_AUTH', value: env.SKOPEO_DEST_CREDENTIALS)]
                pp.resourceRequestCpu = '100m'
                pp.resourceRequestMemory = '256Mi'
                pp.resourceLimitCpu = '1000m'
                pp.resourceLimitMemory = '1Gi'
                if (config.containsKey("pipelinePodName")) {
                    pp.pipelinePodName = config.pipelinePodName
                }

                if (config.containsKey("idleMinutes")) {
                    pp.idleMinutes = config.idleMinutes
                }

            }) {
                body.call()
            }
        }
    } catch (CredentialNotFoundException e) {
        echo "[ERROR] The credentials '${artifactoryBasicOutCredentialsId}' could not be found. Please provide existing credentials to access artifactory."
        throw e
    }
}
