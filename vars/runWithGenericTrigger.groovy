import org.jenkinsci.plugins.workflow.steps.MissingContextVariableException

def call(Map config, Closure body) {


    assert config.containsKey("webhookToken"): "config must define property 'webhookToken'"
    assert config.containsKey("branchFilterExpression"): "config must define property 'branchFilterExpression'"


    if (!config.containsKey("numToKeepStr")) {
        config.numToKeepStr = "5"
    }

    properties([
            buildDiscarder(logRotator(numToKeepStr: config.numToKeepStr)),
            pipelineTriggers([
                    [$class                   : 'GenericTrigger',
                     genericVariables         : [
                             [key: 'committer_name', value: '$.actor.displayName'],
                             [key: 'committer_email', value: '$.actor.emailAddress'],
                             [key: 'ref', value: '$.changes[0].refId'],
                             [key: 'tag', value: '$.changes[0].refId', regexpFilter: 'refs/tags/'],
                             [key: 'commit', value: '$.changes[0].toHash'],
                             [key: 'repo_slug', value: '$.repository.slug'],
                             [key: 'project_key', value: '$.repository.project.key'],
                             [key: 'clone_url', value: '$.repository.links.clone[0].href']
                     ],

                     causeString              : '$committer_name pushed tag $tag to $clone_url referencing $commit',
                     token                    : config.webhookToken,
                     printContributedVariables: true,
                     printPostContent         : true,
                     regexpFilterText         : '$ref',
                     //  match all but 'master'
                     regexpFilterExpression   : config.branchFilterExpression
                    ]
            ])
    ])

    // if branch was deleted
    if (env.commit == "0000000000000000000000000000000000000000") {
        echo "ignore commit ${env.commit}"
        return
    }

    if (env.ref) {
        currentBuild.displayName = "${currentBuild.displayName} ${env.ref}"
    }

    doNotifyBitbucket('INPROGRESS', config.bitbucketCredentialsId, env.commit)
    try {
        body.call(new Checkout(this))
        doNotifyBitbucket('SUCCESSFUL', config.bitbucketCredentialsId, env.commit)
    } catch (err) {
        doNotifyBitbucket('FAILED', config.bitbucketCredentialsId, env.commit)
        throw err
    }
}

void doNotifyBitbucket(String status, String credentialsId, String commitSha1) {
    if (credentialsId) {
        try {
            notifyBitbucket(buildStatus: status, credentialsId: credentialsId, commitSha1: commitSha1)
        } catch (IllegalArgumentException err) {
            // if failed check if node context is missing
            if (err.cause?.cause && err.cause.cause instanceof MissingContextVariableException) {
                node() {
                    notifyBitbucket(buildStatus: status, credentialsId: credentialsId, commitSha1: commitSha1)
                }
            } else {
                throw err
            }
        } catch (MissingContextVariableException err) {
            node() {
                notifyBitbucket(buildStatus: status, credentialsId: credentialsId, commitSha1: commitSha1)
            }
        }
    }
}


class Checkout {
    def steps

    Checkout(steps) {
        this.steps = steps
    }

    void checkout() {
        steps.checkout([
                $class                           : 'GitSCM',
                branches                         : [[name: "${steps.env.ref}"]],
                doGenerateSubmoduleConfigurations: steps.scm.doGenerateSubmoduleConfigurations,
                extensions                       : steps.scm.extensions,
                userRemoteConfigs                : steps.scm.userRemoteConfigs
        ])

    }
}
