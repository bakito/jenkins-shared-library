import com.openshift.jenkins.plugins.OpenShiftDSL
import spock.lang.Specification

import static Pipeline.get
import static org.junit.Assert.assertTrue

class getImageStreamRegistryUrlTest extends Specification {

    def "getImageStreamRegistryUrl"() {

        setup:
        Pipeline pipeline = get(this)

        String isName = "is-^${UUID.randomUUID().toString()}"
        String mockOutput = "url-${UUID.randomUUID().toString()}"

        // mock openshift functions

        def openshift = Mock(OpenShiftDSL)
        def selector = Mock(OpenShiftDSL.OpenShiftResourceSelector)


        pipeline.getBinding().setVariable("openshift", openshift)

        def getImageStreamRegistryUrl = pipeline.loadScript()

        when:
        def result = getImageStreamRegistryUrl(isName)

        then:
        1 * openshift.withCluster(_) >> { Closure c -> c.call() }
        1 * openshift.withProject(_) >> { Closure c -> c.call() }
        1 * openshift.selector(_, _) >> { String kind, String name ->
            return selector
        }
        1 * selector.object() >> {
            return ["status": ["dockerImageRepository": mockOutput]]
        }

        result == mockOutput
    }
}
