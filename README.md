# Shared jenkins function library for Six docker base image builds

## Documentation

[https://jenkins.io/doc/book/pipeline/shared-libraries/](https://jenkins.io/doc/book/pipeline/shared-libraries/)

## Usage

### to include the full function set from six-jenkins-shared-library load the following library:

```groovy
@Library("six-jenkins-shared-library") _
```

### functions

[functions](./vars)
(the name of the file in the jenkins-shared-library is the name of the function)

### to run a function

```groovy
defaultProperties()
```

or:

```groovy
jobContext.registry = getImageStreamRegistryUrl('six-rhel7')
```

### Examples

Example usages can be seen in the [test directory](./test) or [Jenkinsfile](Jenkinsfile)


## Links of other libraries (get some inspiration)

- [https://github.com/fabric8io/fabric8-pipeline-library](https://github.com/fabric8io/fabric8-pipeline-library)
- [https://github.com/fabric8io/fabric8-jenkinsfile-library](https://github.com/fabric8io/fabric8-jenkinsfile-library)
- [https://github.com/domenicbove/openshift-pipeline](https://github.com/domenicbove/openshift-pipeline)
