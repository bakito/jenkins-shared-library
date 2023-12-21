// vars/runBuildConfig.groovy
def call(Map config, Closure modifyBuildConfig = null) {

    assert config.buildConfig != null: "'buildConfig' parameter is required"
    def buildConfig = config.buildConfig
    def dir = null
    def retryTimeout = 1

    if (config.containsKey("dir")) {
        dir = config.dir
    }
    if (config.containsKey("retries")) {
        retries = config.retries
    }
    if (config.containsKey("timeout")) {
        retryTimeout = config.timeout
    }

    def buildArgs = [buildConfig]
    if (dir != null && !dir.trim().equals("")) {
        buildArgs.add("--from-dir=${dir}")
    }

    if (config.containsKey("buildArgs")) {
        if (config.buildArgs instanceof Collection) {
            buildArgs.addAll(config.buildArgs)
        } else {
            buildArgs.add(config.buildArgs)
        }
    }

    openshift.withCluster() {
        openshift.withProject(config.namespace) {

            if (modifyBuildConfig) {
                def bc = openshift.selector("bc/${buildConfig}").object()
                modifyBuildConfig.call(bc)

                echo "Update BuildConfig ${buildConfig}"
                openshift.apply(bc)
            }

            echo "Start BuildConfig ${buildConfig} with args ${buildArgs}"
            def buildSelector = openshift.startBuild(buildArgs)
            buildSelector.logs('-f')
            echo "Get build object of ${buildConfig}"

            def build
            def start = new Date()

            timeout(retryTimeout) {
                waitUntil {
                    script {
                        build = buildSelector.object()
                        echo "Current build status is: ${build.status.phase}"
                        return build.status.phase == "Complete" || build.status.phase == "Failed"
                    }
                }
            }

            if (config.containsKey("buildLogFile")) {
                writeFile file: config.buildLogFile, text: """
Start: ${start}
Build: ${build.metadata.namespace}/${build.metadata.name}
------------------------------------------------------------
${buildSelector.logs().out}
"""
            }

            assert build.status.phase == "Complete": "build failed"
        }
    }
}
