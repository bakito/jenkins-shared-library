import io.fabric8.jenkins.openshiftsync.GlobalPluginConfiguration
import spock.lang.Specification

import static Pipeline.get

class getCredentialsWithNamespaceTest extends Specification {

    def "getCredentialsWithNamespace"() {

        setup:
        def getCredentialsWithNamespace = get(this).loadScript()

        // mock namespace evaluation
        String expectedNS = UUID.randomUUID().toString()
        GlobalPluginConfiguration gc = GroovyMock(GlobalPluginConfiguration, global: true)

        when: "obtaining the credentials with namespace"
        String secret = UUID.randomUUID().toString()
        def cwn = getCredentialsWithNamespace(secret)

        then:
        1 * GlobalPluginConfiguration.get() >> gc
        1 * gc.getNamespace() >> expectedNS

        expect: "the result will be as expected"
        cwn == "${expectedNS}-${secret}"
    }
}
