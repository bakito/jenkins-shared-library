package com.six_group.jenkins.tools

import com.cloudbees.jenkins.plugins.customtools.CustomTool
import com.synopsys.arc.jenkinsci.plugins.customtools.versions.ToolVersionConfig
import hudson.tools.InstallSourceProperty
import hudson.tools.ToolProperty
import hudson.tools.ZipExtractionInstaller
import hudson.tools.CommandInstaller
import jenkins.model.Jenkins

class GlobalTool {
    String name
    String homeVariablePrefix
    String archiveSubdir
    String asFileName
    boolean addToPath = true
    boolean replace = false
    String binDir = "/bin"
    Map additionalVars = [:]

    void init(Map config) {
        if (!config.containsKey("name")) {
            throw new IllegalAccessException("Missing parameter 'name'")
        }
        if (!config.containsKey("homeVariablePrefix")) {
            throw new IllegalAccessException("Missing parameter 'homeVariablePrefix'")
        }

        this.name = config.name
        this.homeVariablePrefix = config.homeVariablePrefix

        copyProperty(config, "addToPath")
        copyProperty(config, "binDir")
        copyProperty(config, "additionalVars")
        copyProperty(config, "replace")
        copyProperty(config, "archiveSubdir")
        copyProperty(config, "asFileName")
    }


    private copyProperty(Map config, String key) {
        if (config.containsKey(key)) {
            this[key] = config[key]
        }
    }

    boolean exists() {
        CustomTool.DescriptorImpl instances = Jenkins.instance.getExtensionList(CustomTool.DescriptorImpl.class)[0]
        return instances.getInstallations().find {
            it.name == this.name
        }
    }

    void setup(steps, Map config) {
        if (!config.containsKey("url")) {
            throw new IllegalAccessException("Missing parameter 'url'")
        }


        CustomTool.DescriptorImpl instances = Jenkins.instance.getExtensionList(CustomTool.DescriptorImpl.class)[0]

        CustomTool existing = instances.getInstallations().find {
            it.name == this.name
        }
        List allInstallations = instances.getInstallations()

        if (this.replace && existing != null) {
            steps.echo "Replacing global tool '${this.name}'"
            allInstallations.remove(existing)
        } else {
            steps.echo "Installing global tool '${this.name}'"
        }

        List installers = new ArrayList()
        String url = config.url
        if (!url.startsWith("https://artifactory.six-group.net/artifactory/")) {
            url = "https://artifactory.six-group.net/artifactory/${url}"
        }
        if (url.endsWith("zip") || url.endsWith("gzip") || url.endsWith("tar.gz")) { // Archive
            installers.add(new ZipExtractionInstaller(null, url, this.archiveSubdir) as Object)
        } else { // Executable
            // The command checks if the tool is already installed, if not installed performs a 'wget' command, renames the file to 'asFileName' and finally adds the permissions to execute the tool
            installers.add(new CommandInstaller(null, "if [ -e ${this.asFileName} ]; then echo \"${this.asFileName} already installed!\"; else wget --no-clobber --no-verbose ${url} --output-document=${this.asFileName} && chmod +x ${this.asFileName}; fi", "./") as Object)
        }

        List<ToolProperty> properties = new ArrayList<ToolProperty>()
        properties.add(new InstallSourceProperty(installers) as ToolProperty)

        def newTool = new CustomTool(this.name, null, properties, "bin", null, ToolVersionConfig.DEFAULT, null)

        allInstallations.add(newTool)

        instances.setInstallations((CustomTool[]) allInstallations)
        instances.save()
    }
}