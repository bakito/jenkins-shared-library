// vars/getGitRevision.groovy
import com.six_group.jenkins.OpenShiftUtils

def branch() {
    utils = new OpenShiftUtils()
    return utils.getGitBranch()
}

def repo() {
    utils = new OpenShiftUtils()
    return utils.getGitRepo()
}

def revision(boolean shortRevision = true) {
    utils = new OpenShiftUtils()
    return utils.getGitRevision(shortRevision)
}

def comment() {
    utils = new OpenShiftUtils()
    return utils.getGitComment()
}

def tag() {
    utils = new OpenShiftUtils()
    return utils.getCurrentGitTag()
}

def should(String key) {
    utils = new OpenShiftUtils()
    return utils.should(key)
}

def shouldNot(String key) {
    utils = new OpenShiftUtils()
    return utils.shouldNot(key)
}
