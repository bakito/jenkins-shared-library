import io.fabric8.jenkins.openshiftsync.GlobalPluginConfiguration
import spock.lang.Specification

import static Pipeline.get

class getNamespaceTest extends Specification {

    def "getNamespace"() {

        setup:
        def getNamespace = get(this).loadScript()
        // mock namespace evaluation
        String expectedNS = UUID.randomUUID().toString()
        GlobalPluginConfiguration gc = GroovyMock(GlobalPluginConfiguration, global: true)


        when: "obtaining the namespace"
        def ns = getNamespace()

        then:
        1 * GlobalPluginConfiguration.get() >> gc
        1 * gc.getNamespace() >> expectedNS


        expect: "the namespace will be as expected"
        ns == expectedNS
    }
}
