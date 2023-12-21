// test with configuration closure; body closure outside of parenthesis
pipelinePod('artifactory.six-group.net/sdbi/six-rhel7', { pp ->
    pp.command = 'cat'
}) {
    echo "With custom configuration; body closure outside of parenthesis"
    sh 'pwd'
}

// test with configuration closure
pipelinePod('artifactory.six-group.net/sdbi/six-rhel7', { pp ->
    pp.command = 'cat'
}, {
    echo "With custom configuration"
    sh 'pwd'
})

// test without configuration closure; body closure outside of parenthesis
pipelinePod('artifactory.six-group.net/sdbi/six-rhel7') {
    echo "With default configuration; body closure outside of parenthesis"
    sh 'pwd'
}

// test without configuration closure
pipelinePod('artifactory.six-group.net/sdbi/six-rhel7', {
    echo "With default configuration"
    sh 'pwd'
})


// test with a maven image
dir('test') {
    stash includes: 'pom.xml', name: 'pom'
}
echo "maven pod"

withCredentials([usernamePassword(credentialsId: getCredentialsWithNamespace('artifactory'), usernameVariable: 'ARTIFACTORY_USERNAME', passwordVariable: 'ARTIFACTORY_PASSWORD')]) {
    pipelinePod("artifactory.six-group.net/sdbi/jenkins-slave-maven:latest", { pp ->

        pp.asJnlpAgent()
        pp.timeoutSeconds = 300
        pp.envVars = [envVar(key: 'ARTIFACTORY_USERNAME', value: env.ARTIFACTORY_USERNAME),
                    envVar(key: 'ARTIFACTORY_PASSWORD', value: env.ARTIFACTORY_PASSWORD)]

    }) {
        unstash 'pom'
        sh 'mvn clean install -B'
    }
}