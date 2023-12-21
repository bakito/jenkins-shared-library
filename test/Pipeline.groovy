import com.lesfurets.jenkins.unit.BasePipelineTest
import com.lesfurets.jenkins.unit.PipelineTestHelper
import io.fabric8.kubernetes.api.model.EnvVar

/**
 * Inspired from @{@link com.lesfurets.jenkins.unit.BasePipelineTest#setUp()}
 */
class Pipeline extends BasePipelineTest {

    private final Object owner

    static Pipeline get(Object owner) {
        return new Pipeline(owner)
    }

    private Pipeline(Object owner) {
        this.owner = owner
        setUp()
        getHelper().registerAllowedMethod("envVar", [Map.class]) { m -> new EnvVar(m["key"], m["value"], null) }
    }

    @Override
    PipelineTestHelper getHelper() {
        return super.getHelper()
    }

    @Override
    Binding getBinding() {
        return super.getBinding()
    }

    Script loadScript() {
        def name = owner.class.getSimpleName().substring(0, owner.class.getSimpleName().indexOf("Test"))
        return getHelper().loadScript("vars/${name}.groovy", getBinding())
    }
}
