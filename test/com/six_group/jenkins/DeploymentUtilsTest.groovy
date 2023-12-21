package com.six_group.jenkins

class DeploymentUtilsTest extends GroovyTestCase {
    void testValidateChangeNo() {
        def utils = new DeploymentUtils(null, null)

        // TODO: enable when real validation is implemented
        //def isValid1 = utils.validateChangeNo('aChange')
        //assert isValid1 == false: 'The changeNo must be valid'

        def isValid2 = utils.validateChangeNo('aChange')
        assert isValid2: 'The changeNo must be valid'
    }

    void testValidateUser() {
        def utils = new DeploymentUtils(null, null)

        def isValid1 = utils.validateUser("tp666")
        assert !isValid1: 'The user must be a personal user'

        def isValid2 = utils.validateUser("tk666")
        assert isValid2: 'The user must be a personal user'

        def isValid3 = utils.validateUser("tx666")
        assert isValid3: 'The user must be a personal user'
    }

    void testDefineStage() {
        def utils = new DeploymentUtils(null, null)

        def stage1 = utils.defineStage('namespace-dev')
        assert stage1 == 'dev': 'The stage must be in the enumeration'

        def stage2 = utils.defineStage('namespace-tst')
        assert stage2 == 'tst': 'The stage must be in the enumeration'

        def stage3 = utils.defineStage('namespace-test')
        assert stage3 == 'tst': 'The stage must be in the enumeration'

        def stage4 = utils.defineStage('namespace-int')
        assert stage4 == 'int': 'The stage must be in the enumeration'

        def stage5 = utils.defineStage('namespace-qa')
        assert stage5 == 'qa': 'The stage must be in the enumeration'

        def stage6 = utils.defineStage('namespace')
        assert stage6 == 'prod': 'The stage must be in the enumeration'

        def stage7 = utils.defineStage('namespace-whatever')
        assert stage7 == 'prod': 'The stage must be in the enumeration'
    }

    void testEncodePassword() {
        def utils = new DeploymentUtils(null, null)
        def encodedPass = utils.encodePassword('p@ssword:with:ch@r@cters*$')
        assert encodedPass == 'p%40ssword%3Awith%3Ach%40r%40cters*%24'
    }
}
