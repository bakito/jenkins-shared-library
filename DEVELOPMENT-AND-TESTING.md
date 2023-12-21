# Development and testing of the shared library

## IDE

The recommended IDE is IntelliJ with gradle support

## Dependencies

If plugins are used in the shared library they should be added to the dependencies as **jenkinsPlugins**  in [build.gradle](./build.gradle)

## Developing tests

Since jenkins is not running during the tests, we decided too have mocked test. Fot that we have different possibilities.

### Mocking DSL functions 
The [Pipeline](./test/Pipeline.groovy) helper class provides a mock the DSL functions.

#### Initialisation 
```groovy

import static Pipeline.get
import spock.lang.Specification

class myTest extends Specification {
    // initialize the pipeline mock helper
    Pipeline pipeline = get(this)
}
```

#### Register function mock 
```groovy
//                                         name of the function, input parameters of the function, Closure to implement the mock function
pipeline.getHelper().registerAllowedMethod("dir",                [String.class, Closure.class],    { t, c ->
    c.call()
    return null
})
```

#### Setting variables
```groovy
//                                         name of the function, input parameters of the function, Closure to implement the mock function
pipeline.getBinding().setVariable("env", [MY_VAR: 'a value'])
```

#### Mocking openshift DSL
See the test [deployApplicationV2Test](./test/deployApplicationV2Test.groovy) as an example.

 

## Running tests

```bash
gradlew.bat test
``` 