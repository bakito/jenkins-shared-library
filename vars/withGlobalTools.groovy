import com.six_group.jenkins.tools.GlobalTool

def call(List<GlobalTool> tools, Closure body) {

    List<String> envs = []
    List<String> paths = []
    for (t in tools) {

        def toolPath = tool name: t.name, type: 'com.cloudbees.jenkins.plugins.customtools.CustomTool'
        envs.add("${t.homeVariablePrefix.toUpperCase()}_HOME=${toolPath}")
        if (t.addToPath) {
            paths.add("${toolPath}${t.binDir}")
        }
        t.additionalVars.each { key, val ->
            if ('PATH' != key) {
                envs.add("${key}=${toolPath}/${val}")
            } else {
                echo "WARNING: ignoring additionalVar 'PATH' of global tool '${t.name}', please use properties 'addToPath' and 'binDir' to configure PATH variable creation."
            }
        }
    }

    // build the PATH variable
    envs.add("PATH+TOOLS=${paths.join(':')}")

    withEnv(envs) {
        body.call()
    }
}