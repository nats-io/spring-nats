# Project Management

This is just to note basic build processes.

### Version Format

The version numbering is a combination of the normal semver plus the spring version,
i.e. `<version>1.2.3+3.1</version>`

### GitHub Actions

There are 2 GitHub actions:

1\. [Build Pull Request](.github/workflows/build-pr.yml) 

When a PR is submitted, this runs to compile and test the project. 

2\. [Build Merge](.github/workflows/build-merge.yml)

When a PR is merged, this runs to compile, test and publish the project. 


### Version Management.

The following pom.xml files all contain the version tag holding the version as shown in the format. 
Some may contain multiple tags due to dependencies.  

* [parent](pom.xml)
* [demo](demo/pom.xml)
* [nats-spring](nats-spring/pom.xml)
* [nats-spring-boot-starter](nats-spring-boot-starter/pom.xml)
* [nats-spring-cloud-stream-binder](nats-spring-cloud-stream-binder/pom.xml)
* [nats-spring-samples parent](nats-spring-samples/pom.xml)
* [nats-spring-samples autoconfigure](nats-spring-samples/autoconfigure-sample/pom.xml)
* [nats-spring-samples connect-error](nats-spring-samples/connect-error-sample/pom.xml)
* [nats-spring-samples listener-sample](nats-spring-samples/listener-sample/pom.xml)
* [nats-spring-samples multi-connect](nats-spring-samples/multi-connect-sample/pom.xml)
* [nats-spring-samples polling](nats-spring-samples/polling-sample/pom.xml)
* [nats-spring-samples processor](nats-spring-samples/processor-sample/pom.xml)
* [nats-spring-samples queue](nats-spring-samples/queue-sample/pom.xml)
* [nats-spring-samples source](nats-spring-samples/source-sample/pom.xml)


All `<version>` tags in these files should match.

i.e. `<version>1.2.3+3.1</version>` or `<version>1.2.3+3.1-SNAPSHOT</version>`

When a PR is merged to main the project will be published either as a snapshot 
if the version contains `-SNAPSHOT`, otherwise a release will be published.

### Workflow

When a version is in progress, all version tags should contain `-SNAPSHOT`, 
that way all merges to main will publish snapshots.

When it's time to release, do the following:

1. Update all version tags to remove `-SNAPSHOT`
2. Push to a branch and make a PR.
3. Merge the PR.

While the release action is building, you can...
1. Update all version tags to increment the semver and add `-SNAPHOT`
2. Push a branch and make a PR.

Once the release action is completed...
1. Merge the PR.
