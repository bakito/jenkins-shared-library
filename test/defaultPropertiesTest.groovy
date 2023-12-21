import spock.lang.Specification

import static Pipeline.get

class defaultPropertiesTest extends Specification {

    Pipeline pipeline = get(this)
    def defaultProperties
    Map context = [:]

    def setup() {

        pipeline.getHelper().registerAllowedMethod("logRotator", [Map.class], { m ->
            context.numToKeepStr = m["numToKeepStr"]
            return null
        })


        defaultProperties = pipeline.loadScript()
    }

    def "defaultProperties_Default"() {

        setup:
        when:
        defaultProperties()

        then: "the namespace will be as expected"
        context.numToKeepStr == '5'
    }

    def "defaultProperties_pollSCM"() {
        setup:
        String cronPollSCM = UUID.randomUUID().toString()
        pipeline.getHelper().registerAllowedMethod("pollSCM", [String]) { c ->
            context.pollSCM = c
        }

        when:
        defaultProperties(10, cronPollSCM)

        then:
        context.numToKeepStr == '10'
        context.pollSCM == cronPollSCM
    }

    def "defaultProperties_map"() {
        setup:

        String cronPollSCM = UUID.randomUUID().toString()
        String cronTrigger = UUID.randomUUID().toString()

        pipeline.getHelper().registerAllowedMethod("pollSCM", [String]) { c ->
            context.pollSCM = c
        }
        pipeline.getHelper().registerAllowedMethod("cron", [String]) { c ->
            context.cron = c
        }
        pipeline.getHelper().registerAllowedMethod("pipelineTriggers", [List.class]) { l ->
            context.size = l.size()
        }

        when:
        defaultProperties(numberToKeep: 7, cronPollSCM: cronPollSCM, cronTrigger: cronTrigger)

        then:
        context.numToKeepStr == '7'
        context.pollSCM == cronPollSCM
        context.cron == cronTrigger
        context.size == 2
    }
}
