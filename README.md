![NATS](images/large-logo.png)

# NATS Spring Support

[![License][License-Image]][License-Url]

* [Using the NATS Modules](#using)
  * [Multiple NATS Connections](#multi)
* [Using the Binder](#binder)
  * [Request-Reply](#reqreply)
* [Configuration](#configure)
* [Samples](#samples)
* [Building This Project](#build)
* [External Resources](#resources)
* [License](#license)

This repository contains two core packages:

* `spring-nats` an implementation of the autoconfigure pattern for a NATS connection using the core [NATS Java Client](https://github.com/nats-io/nats.java)
* `spring-cloud-stream-binder-nats` a spring cloud binder for NATS

A third package `nats-samples` is included to <a href="#samples">demonstrate</a> how the other two modules can be used.

## Using the NATS Modules <a name="using"></a>

To depend on the autoconfigure module, simply add it as a dependency in your pom.xml:

```xml
<dependency>
    <groupId>io.nats</groupId>
    <artifactId>jnats</artifactId>
    <version>2.5.2</version>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-nats</artifactId>
    <version>NNNN</version>
</dependency>
```

This module defines and exports an auto-configuration for NATS Connection objects.

### Multiple NATS Connections <a name="multi"></a>

By default the autoconfigure modules will create a single NATS connection and the binders will create a second one.

> An example multi-binder-sample is in progress, but is not working currently. The named binders are not getting configured by their environments.

## Using the NATS Cloud Stream Binder <a name="binder"></a>

To use the binder, simply define a binding in your application.yml with the type `nats`:

```yaml
spring:
  cloud:
    stream:
      bindings:
        input:
          destination: dataIn
          binder: nats
        output:
          destination: dataOut
          binder: nats
```

and include a dependency on the library:

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-stream-binder-nats</artifactId>
</dependency>
```

See the [Spring Cloud Stream Documentation](https://spring.io/projects/spring-cloud-stream) for information about more complex configurations of the bindings.

The NATS binder leverages the autoconfigure module, or manual configuration to build a NATS connection. Standard properties are used to specify inputs and outputs. Inputs, specified with a destination and group name are mapped to subjects and queue names, with the destination becoming the subject, and the group becoming the queue. Outputs are specified with a destination name that becomes the subject.

Consumers are implemented with a dispatcher. Each consumer will create its own dispatcher in the core library, resulting in a thread per consumer.

Polled consumers are implemented with a subscription.

> Currently the polling code will wait forever for a message and is not configurable.

Producers publish directly through the connection.

### Request-Reply <a name="reqreply"></a>

The binder uses message header propagation to support NATS-style request-reply. The reply-to subject is passed as a header when a message is received. That reply to header is used as the outgoing subject if it is present when a message is sent. This system by-passes the configured producer destination.

## Configuration <a name="configure"></a>

By default, properties are configured using the `spring.nats` prefix:

* `spring.nats.server` specifies the NATS server url or a list of urls in a comma separated list

## Samples <a name="samples"></a>

This repo provides samples for the major use-cases implemented by the core code:

* [autoconfigure-sample](./nats-samples/autoconfigure-sample) a simple command line runner with spring boot that uses the auto-configured nats connection from an application.properties file.

* [listener-sample](./nats-samples/listener-sample) uses the binder to listen to a single subject and print messages it receives. The configuration in application.yml specifies a single subject `dataIn`.

* [multi-binding-sample](./nats-samples/multi-binding-sample) creates a processor. The example listens to the subject `dataIn` and sends to the subject `dataOut`. If the message contains a UTF-8 string, it is converted to all CAPS before being sent.

* [polling-sample](./nats-samples/polling-sample) is similar to the listener sample, but uses polling.

* [source-sample](./nats-samples/source-sample) generates messages on a timer and sends them to a NATS endpoint. In order to leverage this pattern you need to create a named source. See the `application.yml` for details.

* [queue-sample](./nats-samples/queue-sample) listens on a subject and queue group, run multiple copies of the sample to see how messages are load balanced.

* [multi-connect-sample](./nats-samples/multi-connect-sample) copy of the multi-binding-sample that uses 2 nats connections. **This sample is not working, something is broken with multi-binder support**

You can exercise the samples using the `nats-sub` and `nats-pub` executables for the client library. For example, to try out the listener:

```bash
% java -jar nats-samples/listener-sample/target/listener-sample-0.0.1-SNAPSHOT.jar --spring.nats.server="nats://localhost:4222"
...
2019-06-24 15:36:43.690  INFO 36282 --- [         nats:3] o.s.cloud.stream.binder.nats.Listener    : received message hello
```

```bash
% nats-pub dataIn "hello"
```

For the multi-binder, try:

```bash
% java -jar nats-samples/multi-binding-sample/target/multi-binding-sample-0.0.1.SNAPSHOT.jar --spring.nats.server="nats://localhost:4222"
...

```

```bash
% nats-pub dataIn "hello"
```

```bash
 % nats-sub ">"
Listening on [>]
...
[6] Received on [dataIn]: 'hello'
[7] Received on [dataOut]: 'HELLO'
```

## Building This Project <a name="build"></a>

This project is built with maven. The `mvnw` helper is included in the root folder and its implementation is included in the `.mvn` folder. You should be able to compile using `./mvnw clean compile`, or package the jars with `./mvnw clean package`.

Internally there are multiple pom files, one parent for the project, one parent for the samples, one for the autoconfigure code, one for the binder, and one each for the samples. When built, each will have its own artifacts.

Signing and deploying requires that you set up your settings.xml file for maven:

```xml
<settings>
  <servers>
    <server>
      <id>ossrh</id>
      <username>xxx</username>
      <password>xxxxx</password>
    </server>
  </servers>
  <profiles>
        <profile>
            <activation>
                <activeByDefault>true</activeByDefault>
            </activation>
            <properties>
                <gpg.keyid>xxx</gpg.keyid>
                <gpg.passphrase>xxxxx</gpg.passphrase>
                <gpg.secretkeyring>xxxxx</gpg.secretkeyring>
            </properties>
        </profile>
    </profiles>
</settings>
```

### Adding New Configuration Properties

All of the configuration properties are injected into the NatsProperties class in the autoconfigure module. This class provides a `toOptions()` method that spits out a valid options configuration from the NATS client library. To add new properties, simply add them to NatsProperties and update the `toOptions()` method appropriately.

## External Resources <a name="resources"></a>

* [NATS](https://nats.io/)
* [NATS server](https://github.com/nats-io/gnatsd)
* [NATS Java Client](https://github.com/nats-io/nats.java)
* [Spring Cloud Stream](https://spring.io/projects/spring-cloud-stream)

[License-Url]: https://www.apache.org/licenses/LICENSE-2.0
[License-Image]: https://img.shields.io/badge/License-Apache2-blue.svg

## License <a name="license"></a>

Unless otherwise noted, the nats-account-server source files are distributed under the Apache Version 2.0 license found in the LICENSE file.
