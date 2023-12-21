def namespace = 'six-baseimages-unittest'
def gitRepo = '/opspoc/ocp-deployment-service.git'
def gitTagDeploy = 'unittest/deployment'
def gitTagRedeploy = 'unittest/redeployment'
def gitTagUndeployment = 'unittest/undeployment'
def changeNo = 'aChange'

withCredentials([usernamePassword(credentialsId: getCredentialsWithNamespace('sdbi-unittest'), usernameVariable: 'ARTIFACTORY_USERNAME', passwordVariable: 'ARTIFACTORY_PASSWORD')]) {
    echo('deploy project - prod')
    deployApplication(changeNo, namespace, gitRepo, gitTagDeploy, env.ARTIFACTORY_USERNAME, env.ARTIFACTORY_PASSWORD)

    echo('redeploy project - prod')
    deployApplication(changeNo, namespace, gitRepo, gitTagRedeploy, env.ARTIFACTORY_USERNAME, env.ARTIFACTORY_PASSWORD)

    echo('redeploy project - dev')
    deployApplication(changeNo, namespace, gitRepo, gitTagRedeploy, 'sdbi-bitbucket')

    echo('redeploy project - dev - bitbucket-secret')
    deployApplication(changeNo, namespace, gitRepo, gitTagRedeploy)

    echo('undeploy project - prod')
    deployApplication(changeNo, namespace, gitRepo, gitTagUndeployment, env.ARTIFACTORY_USERNAME, env.ARTIFACTORY_PASSWORD)
}
