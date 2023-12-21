@Library("six-jenkins-shared-library") _
node() {
    pipelinePod("artifactory.six-group.net/sdbi/ubi-zulu-openjdk-jdk-16:latest", { pp ->
        // since the java image has not default command defined - use 'cat' as blocking process otherwise the agent pod is immediately terminated.
        pp.command = 'cat'
        pp.resourceRequestCpu = '1'
        pp.resourceLimitCpu = '2'
        // optional - define a shared volume to be used as maven cache dir
        pp.volumes = [persistentVolumeClaim(claimName: 'maven-home', mountPath: '/.m2/', readOnly: false)]
    }) {
        def MAVEN_VERSION = "3.8.1"
        maven = globalTool(
                name: MAVEN_VERSION,
                homeVariablePrefix: 'M2',
                binDir: '/bin',
                url: "opensource-generic-release-local/maven/${MAVEN_VERSION}/apache-maven-${MAVEN_VERSION}-bin.tar.gz",
                archiveSubdir: "apache-maven-${MAVEN_VERSION}"
        )

        withGlobalTools([maven]) {
            sh 'java -version'
            sh 'mvn -version'
        }
    }
}

