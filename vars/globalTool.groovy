import com.six_group.jenkins.tools.GlobalTool

def call(Map config) {
    GlobalTool gt = new GlobalTool()
    gt.init(config)
    if (gt.replace || !gt.exists()) {
        // If it does not exist, set it up
        gt.setup(this, config)
    }
    return gt
}