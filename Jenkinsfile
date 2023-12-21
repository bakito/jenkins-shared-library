import groovy.transform.Field

@Field
def jobContext

pipeline {
    agent none
    options {
        disableConcurrentBuilds()
        skipDefaultCheckout()
        timestamps()
    }

    stages {
        stage('Setup') {
            agent any
            steps {
                script {
                    checkout scm
                    gitRevision = sh(returnStdout: true, script: 'git rev-parse --verify HEAD').trim()
                    library "six-jenkins-shared-library@${gitRevision}"
                    defaultProperties(numberToKeep: 5)
                    jobContext = getInitialJobContext()
                    jobContext.gitRevision = gitRevision
                }
            }
        }
        stage('Gradle Tests') {
            steps {
                script {
                    pipelinePod("artifactory.six-group.net/sdbi/jenkins-slave-maven:v3.11", { pp ->
                        pp.asJnlpAgent()
                        pp.envVars = [envVar(key: 'GRADLE_OPTS', value: '-Dorg.gradle.daemon=false -Dorg.gradle.warning.mode=none')]
                        pp.volumes = [persistentVolumeClaim(claimName: 'gradle-home', mountPath: '/home/jenkins/.gradle', readOnly: false)]
                    }) {
                        checkout scm

                        currentBuild.displayName = "${currentBuild.displayName} ${currentGit.branch()}"
                        sh './gradlew cobertura --warning-mode=none --stacktrace'
                        junit allowEmptyResults: false, testResults: "build/test-results/**/*.xml"
                        cobertura coberturaReportFile: 'build/reports/cobertura/coverage.xml', zoomCoverageChart: false
                    }
                }
            }
        }
        stage('uow Test (deprecated)') {
            when {
                expression { env.SKIP_UOW != 'true' }
            }
            agent any
            steps {
                script {
                    echo "uow Deploy"
                    deployApplication(changeNo: 'aChange',
                            namespace: 'six-baseimages-unittest',
                            repo: currentGit.repo(),
                            tag: currentGit.branch(),
                            secretName: 'sdbi-bitbucket',
                            uowSubPath: 'test/testdata',
                            failV1ToV2ConversionError: true)

                    echo "uow Redeploy with username/password"
                    withCredentials([usernamePassword(credentialsId: getCredentialsWithNamespace('sdbi-unittest'), usernameVariable: 'STASH_USERNAME', passwordVariable: 'STASH_PASSWORD')]) {
                        deployApplication(changeNo: 'aChange',
                                namespace: 'six-baseimages-unittest',
                                repo: '/sdbi/jenkins-shared-library',
                                tag: currentGit.branch(),
                                username: env.STASH_USERNAME,
                                password: env.STASH_PASSWORD,
                                uowSubPath: 'test/testdata',
                                enabledDeployments: ["test-application"])
                    }

                    echo "uow enabledDeployments"
                    deployApplication(changeNo: 'aChange',
                            namespace: 'six-baseimages-unittest',
                            repo: currentGit.repo(),
                            tag: currentGit.branch(),
                            secretName: 'sdbi-bitbucket',
                            uowSubPath: 'test/testdata',
                            enabledDeployments: ["non-existing-test-application"])

                    echo "uow skipUndeploy"
                    deployApplication(changeNo: 'aChange',
                            namespace: 'six-baseimages-unittest',
                            repo: currentGit.repo(),
                            tag: currentGit.branch(),
                            secretName: 'sdbi-bitbucket',
                            uowSubPath: 'test/testdata',
                            skipUndeploy: true)

                    echo "uow skipDeploy"
                    deployApplication(changeNo: 'aChange',
                            namespace: 'six-baseimages-unittest',
                            repo: currentGit.repo(),
                            tag: currentGit.branch(),
                            secretName: 'sdbi-bitbucket',
                            uowSubPath: 'test/testdata',
                            skipDeploy: true)

                    echo "uow Undeploy"
                    deployApplication(changeNo: 'aChange',
                            namespace: 'six-baseimages-unittest',
                            repo: currentGit.repo(),
                            tag: currentGit.branch(),
                            secretName: 'sdbi-bitbucket',
                            uowSubPath: 'test/testdata',
                            skipDeploy: true)
                }
            }
        }

        stage('uowV2 Test') {
            when {
                expression { env.SKIP_UOW != 'true' }
            }
            agent any
            steps {
                script {
                    echo "uowV2 Deploy"
                    deployApplicationV2(changeNo: 'aChange',
                            namespace: 'six-baseimages-unittest',
                            repo: currentGit.repo(),
                            tag: currentGit.branch(),
                            secretName: 'sdbi-bitbucket',
                            uowSubPath: 'test/testdata')

                    echo "uowV2 Redeploy with username/password"
                    withCredentials([usernamePassword(credentialsId: getCredentialsWithNamespace('sdbi-unittest'), usernameVariable: 'STASH_USERNAME', passwordVariable: 'STASH_PASSWORD')]) {
                        deployApplicationV2(changeNo: 'aChange',
                                namespace: 'six-baseimages-unittest',
                                repo: '/sdbi/jenkins-shared-library',
                                tag: currentGit.branch(),
                                username: env.STASH_USERNAME,
                                password: env.STASH_PASSWORD,
                                uowSubPath: 'test/testdata',
                                enabledDeployments: ["test-application"])
                    }

                    echo "uow enabledDeployments"
                    deployApplicationV2(changeNo: 'aChange',
                            namespace: 'six-baseimages-unittest',
                            repo: currentGit.repo(),
                            tag: currentGit.branch(),
                            secretName: 'sdbi-bitbucket',
                            uowSubPath: 'test/testdata',
                            enabledDeployments: ["non-existing-test-application"])

                    echo "uow skipUndeploy"
                    deployApplicationV2(changeNo: 'aChange',
                            namespace: 'six-baseimages-unittest',
                            repo: currentGit.repo(),
                            tag: currentGit.branch(),
                            secretName: 'sdbi-bitbucket',
                            uowSubPath: 'test/testdata',
                            skipUndeploy: true)

                    echo "uow skipDeploy"
                    deployApplicationV2(changeNo: 'aChange',
                            namespace: 'six-baseimages-unittest',
                            repo: currentGit.repo(),
                            tag: currentGit.branch(),
                            secretName: 'sdbi-bitbucket',
                            uowSubPath: 'test/testdata',
                            skipDeploy: true)
                }
            }
        }
        stage('Helm Test') {
            when {
                expression { env.SKIP_UOW != 'true' }
            }
            agent any
            steps {
                script {
                    echo "uow2helm"
                    uow2helm(namespace: 'six-baseimages-unittest',
                            repo: currentGit.repo(),
                            tag: currentGit.branch(),
                            secretName: 'sdbi-bitbucket',
                            uowSubPath: 'test/testdata')

                    stash name: 'helm-chart', includes: 'helm-chart-test-application.zip'

                    def helmVersion = 'v3.0.0'
                    def helm = globalTool(
                            name: "helm-${helmVersion}",
                            homeVariablePrefix: 'HELM',
                            binDir: '',
                            url: "opensource-generic-release-local/helm/${helmVersion}/helm-${helmVersion}-linux-amd64.tar.gz",
                            archiveSubdir: 'linux-amd64'
                    )
                    withGlobalTools([helm]) {
                        unstash 'helm-chart'
                        unzip dir: 'helm-chart', glob: '', zipFile: 'helm-chart-test-application.zip'
                        dir('helm-chart') {
                            openshift.withCluster() {
                                openshift.withProject('six-baseimages-unittest') {
                                    sh 'oc project six-baseimages-unittest'
                                    // be sure the app is not installed
                                    sh returnStatus: true, script: 'helm delete test-application'
                                    sh 'helm install test-application .'
                                    sh 'helm list'
                                    sh 'helm delete test-application'
                                    sh 'helm list'
                                }
                            }
                        }
                    }

                    def helmFileVersion = 'v0.142.0'
                    def helmFile = globalTool(
                            name: "helmfile-${helmFileVersion}",
                            homeVariablePrefix: 'HELMFILE',
                            url: "opensource-generic-release-local/helmfile/${helmFileVersion}/helmfile_linux_amd64",
                            binDir: '',
                            asFileName: 'helmfile'
                    )
                    withGlobalTools([helmFile]) {
                        openshift.withCluster() {
                            openshift.withProject('six-baseimages-unittest') {
                                sh returnStatus: true, script: 'helmfile  --version'
                            }
                        }
                    }

                }
            }
        }

        stage('Merge develop to master') {
            agent any
            when {
                expression { currentGit.branch() == 'develop' }
            }
            steps {
                sshagent([getCredentialsWithNamespace('sdbi-bitbucket')]) {
                    sh 'rm -Rf merge-checkout'
                    sh 'git clone ssh://git@stash.six-group.net:22/sdbi/jenkins-shared-library.git merge-checkout'
                    dir('merge-checkout') {
                        sh "git push origin ${jobContext.gitRevision}:master"
                    }
                }
            }
        }
    }
}
