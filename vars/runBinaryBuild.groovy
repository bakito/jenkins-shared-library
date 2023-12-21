// vars/runBinaryBuild.groovy
def call(Map config) {

    echo "DEPRECATED: use \"runBuildConfig(buildConfig: '<build-config-name>', dir: '.')\" instead"

    if (!config.containsKey("dir")) {
        config.dir = "."
    }
    runBuildConfig(config)
}