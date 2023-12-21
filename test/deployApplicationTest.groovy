// Don't know why intellij doesn't resolve this dependency
import com.openshift.jenkins.plugins.OpenShiftDSL
import groovy.json.JsonSlurper
import spock.lang.Specification

import static Pipeline.get

class deployApplicationTest extends Specification {

    def namespace = 'six-baseimages-unittest'
    def gitRepo = '/opspoc/ocp-deployment-service.git'
    def gitTagDeploy = 'unittest/deployment'
    def changeNo = 'aChange'


    def "deploy project"() {

        setup:
        Pipeline pipeline = get(this)


        pipeline.getHelper().registerAllowedMethod("echo", [String.class], { s ->
            println "${s}"
            return null
        })
        pipeline.getHelper().registerAllowedMethod("pwd", [], { s ->
            return "."
        })
        pipeline.getHelper().registerAllowedMethod("readJSON", [Map.class], { m ->
            if (m.text != null) {
                return [:]
            }
            def name = m.file.substring(m.file.lastIndexOf('/') + 1)
            def inputFile = new File("./test/testdata/${name}")
            def inputJSON = new JsonSlurper().parseText(inputFile.text)
            return inputJSON
        })
        pipeline.getHelper().registerAllowedMethod("writeFile", [Map.class], null)
        pipeline.getHelper().registerAllowedMethod("writeYaml", [Map.class], null)
        pipeline.getHelper().registerAllowedMethod("zip", [Map.class], null)
        pipeline.getHelper().registerAllowedMethod("readFile", [Map.class], { m ->
            def name = m.file.substring(m.file.lastIndexOf('/') + 1)
            def inputFile = new File("./test/testdata/${name}")
            return inputFile.text
        })
        pipeline.getHelper().registerAllowedMethod("readProperties", [Map.class], { m ->
            def name = m.file.substring(m.file.lastIndexOf('/') + 1)
            Properties properties = new Properties()
            File propertiesFile = new File("./test/testdata/${name}")
            propertiesFile.withInputStream {
                properties.load(it)
            }
            return properties
        })
        pipeline.getHelper().registerAllowedMethod("timeout", [Integer.class, Closure.class], { t, c ->
            c.call()
            return null
        })
        pipeline.getHelper().registerAllowedMethod("dir", [String.class, Closure.class], { t, c ->
            c.call()
            return null
        })
        pipeline.getHelper().registerAllowedMethod("wrap", [Map.class, Closure.class], { t, c ->
            c.call()
            return null
        })
        pipeline.getHelper().registerAllowedMethod("addBadge", [Map.class], null)
        pipeline.getHelper().registerAllowedMethod("addInfoBadge", [Map.class], null)
        pipeline.getHelper().registerAllowedMethod("addErrorBadge", [Map.class], null)
        pipeline.getHelper().registerAllowedMethod("addWarningBadge", [Map.class], null)
        pipeline.getHelper().registerAllowedMethod("removeBadges", [Map.class], null)
        pipeline.getHelper().registerAllowedMethod("createSummary", [Map.class], { m ->
            return new MockSummary()
        })

        pipeline.getBinding().setVariable("env", [BUILD_URL: "xxx"])

        // mock openshift functions

        def openshift = Mock(OpenShiftDSL)
        def selector = Mock(OpenShiftDSL.OpenShiftResourceSelector)
        def dcSelector = Mock(OpenShiftDSL.OpenShiftResourceSelector)
        def rollout = Mock(OpenShiftDSL.OpenShiftRolloutManager)


        pipeline.getBinding().setVariable("openshift", openshift)


        def deployApplication = pipeline.loadScript()

        when:
        deployApplication(changeNo, namespace, gitRepo, gitTagDeploy, "", "")


        then:
        1 * openshift.withCluster(_) >> { Closure c -> c.call() }
        1 * openshift.withProject(_, _) >> { String s, Closure c -> c.call() }
        13 * openshift.selector(_, _) >> { String kind, String name -> // 2 times (undeploy and deploy ) for each component, rollout not supported in V1
            if (kind == "DeploymentConfig") {
                return dcSelector
            }
            return selector
        }

        1 * openshift.create(_) // 1 time for dc
        5 * openshift.apply(_) // 1 time for each component (excluding dc)
        5 * openshift.delete(_) // 1 time for each component (excluding dc)

        10 * selector.exists() >> true // 2 times (undeploy and deploy ) for each component
        0 * selector.rollout() // only dc gets rolled out

        3 * dcSelector.exists() >>> [false, false, true] // 3 times for dc: undeploy, deploy, rollout
        2 * dcSelector.rollout() >> rollout

        1 * rollout.latest() // 1 time for dc
        1 * rollout.status() // 1 time for dc
    }
}
