def testRhel() {
    echo "PipelinePod rhel"
    pp = new com.six_group.jenkins.PipelinePod(this, "artifactory.six-group.net/sdbi/six-rhel7:latest")
    pp.command = 'cat'
    pp.timeoutSeconds = 300
    pp.execute() {
        sh 'which fix-permissions.sh'
    }
}

def testMvn() {
    echo "PipelinePod mvn slave"

    String javaOpts = "${UUID.randomUUID()}"

    pp = new com.six_group.jenkins.PipelinePod(this, "artifactory.six-group.net/sdbi/jenkins-slave-maven:latest")

    pp.asJnlpAgent()

    pp.timeoutSeconds = 300
    pp.envVars = [envVar(key: 'JAVA_OPTS', value: javaOpts), envVar(key: 'FOO', value: 'BAR')]

    pp.execute() {
        sh 'which mvn'
        sh 'mvn -version'
        sh 'env | sort'
        String effectiveOpts = sh(returnStdout: true, script: 'echo ${JAVA_OPTS}').trim()
        assert javaOpts == effectiveOpts: "JAVA_OPTS expected '${javaOpts}', got '${effectiveOpts}'"

        String effectiveFoo = sh(returnStdout: true, script: 'echo ${FOO}').trim()
        assert 'BAR' == effectiveFoo: "FOO expected 'BAR', got '${effectiveFoo}'"
    }
}

def runTests() {
    testRhel()
    testMvn()
}

return this