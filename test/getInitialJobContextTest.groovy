import spock.lang.Specification

import static Pipeline.get

class getInitialJobContextTest extends Specification {

    Pipeline pipeline = get(this)
    def getInitialJobContext

    def setup() {
        getInitialJobContext = pipeline.loadScript()
    }

    def "getInitialJobContext_Default"() {

        setup:
        String token = UUID.randomUUID().toString()
        pipeline.getHelper().registerAllowedMethod("sh", [Map.class]) { token }

        when:
        Map ctx = getInitialJobContext()

        then:
        ctx.size() == 1
        ctx.currentBuildVersion == token
    }

    def "getInitialJobContext_NoVersion"() {

        when:
        Map ctx = getInitialJobContext(false)

        then:
        ctx.isEmpty()
    }
}
