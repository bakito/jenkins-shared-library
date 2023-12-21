import com.six_group.jenkins.PipelinePod
import io.fabric8.kubernetes.api.model.EnvVar
import spock.lang.Specification

import static Pipeline.get

class imageMgmtNodeTest extends Specification {

    Pipeline pipeline = get(this)
    def imageMgmtNode
    Map context = [:]

    def setup() {
        imageMgmtNode = pipeline.loadScript()
    }

    def "imageMgmtNode with custom credentials"() {

        setup:
        String credentials = UUID.randomUUID().toString()
        String dockerToken = UUID.randomUUID().toString()
        String skopeoCred = UUID.randomUUID().toString()
        pipeline.getHelper().registerAllowedMethod("usernameColonPassword", [Map.class]) { null }
        pipeline.getHelper().registerAllowedMethod("pipelinePod", [String.class, Closure.class, Closure.class]) { image, setup, body ->
            context.image = image
            context.pp = new PipelinePod(pipeline.getBinding(), image)
            setup.call(context.pp)
            return null
        }
        pipeline.getHelper().registerAllowedMethod("getDockerToken", []) { dockerToken }
        pipeline.getBinding().setVariable("env", [SKOPEO_DEST_CREDENTIALS: skopeoCred])

        when:
        imageMgmtNode(credentials) {}

        then:
        context.image == 'artifactory.six-group.net/sdbi/jenkins-slave-image-mgmt:latest'
        context.pp.timeoutSeconds == 300
        context.pp.envVars.size() == 3
        for (ev in context.pp.envVars) {
            switch (ev.name) {
                case 'SKOPEO_SRC_CREDENTIALS':
                    ev.value == dockerToken
                    break
                case 'SKOPEO_DEST_CREDENTIALS':
                    ev.value == skopeoCred
                    break
                case 'ARTIFACTORY_BASIC_AUTH':
                    ev.value == skopeoCred
                    break
            }
        }
    }

}
