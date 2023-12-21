// vars/getImageStreamRegistryUrl.groovy
def call(String imageStreamName) {
    def url = "N/A"
    openshift.withCluster() {
        openshift.withProject() {
            url = openshift.selector('is', imageStreamName).object().status.dockerImageRepository
        }
    }
    return url
}
