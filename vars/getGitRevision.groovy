// vars/getGitRevision.groovy
import com.six_group.jenkins.OpenShiftUtils
def call(boolean shortRevision = true) {
    echo "WARNING getGitRevision is deprecated. please use 'currentGit.revision()'"
    utils = new OpenShiftUtils()
    return utils.getGitRevision(shortRevision)
}
