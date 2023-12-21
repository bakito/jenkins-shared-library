import spock.lang.Specification

import static Pipeline.get

class getDockerTokenTest extends Specification {

    Pipeline pipeline = get(this)
    def getDockerToken

    String token = UUID.randomUUID().toString()

    def setup() {
        pipeline.getHelper().registerAllowedMethod("sh", [Map.class]) { token }
        getDockerToken = pipeline.loadScript()
    }

    def "getDockerToken_Default"() {

        when:
        def result = getDockerToken()

        then:
        "serviceaccount:" + token == result
    }

    def "getDockerToken_WithLogin"() {

        when:
        def result = getDockerToken('myLogin')

        then:
        "myLogin:" + token == result
    }

    def "getDockerToken_NoLogin"() {

        when: "obtaining the namespace"
        def result = getDockerToken(null)

        then: "the namespace will be as expected"
        token == result
    }
}