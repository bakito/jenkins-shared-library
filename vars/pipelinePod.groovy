import com.six_group.jenkins.PipelinePod

// vars/pipelinePod.groovy
def call(String image, Closure setup = null, Closure body) {
    pp = new PipelinePod(this, image)
    if (setup) {
        setup.call(pp)
    }
    pp.execute() {
        body.call()
    }
}