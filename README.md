![NATS](images/large-logo.png)

# NATS Spring Support

[![License][License-Image]][License-Url]

* [Using the NATS Modules](#using)
  * [Multiple NATS Connections](#multi)
* [Using the Binder](#binder)
  * [Request-Reply](#reqreply)
  * [Partitions](#partition)
* [Configuration](#configure)
  * [Custom Listeners](#listener)
* [Samples](#samples)
* [Building This Project](#build)
* [External Resources](#resources)
* [License](#license)

This repository contains two core packages:

* `nats-spring` an implementation of the autoconfigure pattern for a NATS connection using the core [NATS Java Client](https://github.com/nats-io/nats.java)
* `nats-spring-cloud-stream-binder` a spring cloud binder for NATS

A third package `nats-samples` is included to <a href="#samples">demonstrate</a> how the other two modules can be used.

## Version Notes

As of version 0.3.x the properties used to [configure](#configure) can be in YAML or properties if you pass in the NATS properties externally. If you want the application file to contain connection info it should be a properties file and not YAML. In other words:

```yaml
spring.cloud.stream.bindings.input.destination=dataIn
spring.cloud.stream.bindings.input.binder=nats1
spring.cloud.stream.binders.nats1.type=nats
spring.cloud.stream.binders.nats1.environment.nats.spring.cloud.stream.binder.server=nats://localhost:4222
```

works while the YAML equivalent will not.

Snapshots are hosted on `oss.sonatype.org`, to access these within maven update your settings to include:

```xml
<profiles>
  <profile>
     <id>allow-snapshots</id>
        <activation><activeByDefault>true</activeByDefault></activation>
     <repositories>
       <repository>
         <id>snapshots-repo</id>
         <url>https://oss.sonatype.org/content/repositories/snapshots</url>
         <releases><enabled>false</enabled></releases>
         <snapshots><enabled>true</enabled></snapshots>
       </repository>
     </repositories>
   </profile>
</profiles>
```

The released version should be available at:

```xml
<repository>
  <id>oss-sonatype</id>
  <name>oss-sonatype</name>
  <url>https://oss.sonatype.org/content/repositories/</url>
</repository>
```

and ultimately at maven central.

## Using the NATS Modules <a name="using"></a>

To depend on the autoconfigure module, simply add it as a dependency in your pom.xml:

```xml
<dependency>
    <groupId>io.nats</groupId>
    <artifactId>jnats</artifactId>
    <version>2.5.2</version>
</dependency>
<dependency>
  <groupId>io.nats</groupId>
  <artifactId>nats-spring</artifactId>
  <version>0.4.0-SNAPSHOT</version>
</dependency>
```

This module defines and exports an auto-configuration for NATS Connection objects.

### Multiple NATS Connections <a name="multi"></a>

By default the autoconfigure modules will create a single NATS connection and the binders will create a second one.

> An example multi-binder-sample demonstrates how to have multiple named binders.

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
  <groupId>io.nats</groupId>
  <artifactId>nats-spring-cloud-stream-binder</artifactId>
  <version>0.4.0-SNAPSHOT</version>
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

### Partitions <a name="partition"></a>

NATS does not use the concept of partitions. The binder supports the API for partitions as far as required, but no underlying messaging is associated with that change beyond the name of the destination.

## Configuration <a name="configure"></a>

By default, properties are configured using the `nats.spring` prefix:

* `nats.spring.server` specifies the NATS server url or a list of urls in a comma separated list
* `nats.spring.connectionname` the connection name
* `nats.spring.maxreconnect` the maximum reconnects attempts on a single disconnect before the connection closes
* `nats.spring.reconnectwait` the time, as a duration like `4s`, to wait between trying to reconnect to the same server
* `nats.spring.connectiontimeout` the time, as a duration like `4s`, to wait before cancelling the connection
* `nats.spring.pinginterval` the time, as a duration like `4s`, between pings to the server
* `nats.spring.reconnectbuffersize` the size in bytes for the reconnect buffer
* `nats.spring.inboxprefix` a custom inbox prefix
* `nats.spring.noecho` turn off echo from the server
* `nats.spring.utf8support` enable UTF-8 subject names (warning this is an experimental feature, not all language clients will support it)
* `nats.spring.username`, `nats.spring.password` the user name and password to authenticate with
* `nats.spring.token` an authentication token, takes precedence over the username/password
* `nats.spring.credentials` a path to a credentials file, takes precedence over the token and user/pass

TLS can be configured several ways. Set up a default context using system properties like `javax.net.ssl.keyStore`, set a default SSLContext in the main method before running the spring application, or by setting several properties:

* `nats.spring.keystorepath` the path to the key store file
* `nats.spring.keystorepassword` the password for the key store, defaults to ""
* `nats.spring.keystoretype` (optional) the format of the key store, defaults to "SunX509"
* `nats.spring.truststorepath` the path to the trust store file
* `nats.spring.truststorepassword` the password for the trust store, defaults to ""
* `nats.spring.truststoretype` (optional) the format of the trust store, defaults to "SunX509"

The keyStorePath and trustStorePath must be non-empty to trigger the creation of an SSL context.

### Custom Listeners <a name="listeners"></a>

Custom ConnectionListener and ErrorListeners can be provided by a bean factory. For example, you can implement:

```java
@Bean
public ConnectionListener createConnectionListener() {
  return new ConnectionListener() {
    @Override
    public void connectionEvent(Connection conn, Events type) {
        System.out.println("## Custom status change " + type);
        }
  };
}
```

to create a custom connection listener. The [connect-error-sample](./nats-samples/connect-error-sample) has an example for both types of listener.

If no custom listeners are provided, a default one is used which will log errors and connection events.

## Samples <a name="samples"></a>

This repo contains two types of samples. First there is a [stand-alone demo](demo/README.md) that can be used as starter code, for a POM at least. Second there is a collection of samples showing the major use-cases implemented by the core code:

* [autoconfigure-sample](./nats-samples/autoconfigure-sample) a simple command line runner with spring boot that uses the auto-configured nats connection from an application.properties file.

* [listener-sample](./nats-samples/listener-sample) uses the binder to listen to a single subject and print messages it receives. The configuration in application.yml specifies a single subject `dataIn`.

* [processor-sample](./nats-samples/processor-sample) creates a processor. The example listens to the subject `dataIn` and sends to the subject `dataOut`. If the message contains a UTF-8 string, it is converted to all CAPS before being sent.

* [polling-sample](./nats-samples/polling-sample) is similar to the listener sample, but uses polling.

* [source-sample](./nats-samples/source-sample) generates messages on a timer and sends them to a NATS endpoint. In order to leverage this pattern you need to create a named source. See the `application.yml` for details.

* [queue-sample](./nats-samples/queue-sample) listens on a subject and queue group, run multiple copies of the sample to see how messages are load balanced.

* [multi-connect-sample](./nats-samples/multi-connect-sample) copy of the processor-sample that uses 2 nats connections.

* [connect-error-sample](./nats-samples/connect-error-sample) demonstrates how to set up a custom connection and error handler.

You can exercise the samples using the `nats-sub` and `nats-pub` executables for the client library. For example, to try out the listener:

```bash
% java -jar nats-samples/listener-sample/target/listener-sample-0.4.0-SNAPSHOT.jar --nats.spring.server="nats://localhost:4222"
...
2019-06-24 15:36:43.690  INFO 36282 --- [         nats:3] o.s.cloud.stream.binder.nats.Listener    : received message hello
```

```bash
% nats-pub dataIn "hello"
```

For the multi-binder, try:

```bash
% java -jar nats-samples/processor-sample/target/processor-sample-0.4.0-SNAPSHOT.jar --nats.spring.server="nats://localhost:4222"
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

**Sonatype will accept and close a non-staging repository, but you must manually release it.**

### Adding New Configuration Properties

All of the configuration properties are injected into the NatsProperties class in the autoconfigure module. This class provides a `toOptions()` method that spits out a valid options configuration from the NATS client library. To add new properties, simply add them to NatsProperties and update the `toOptions()` method appropriately.

## External Resources <a name="resources"></a>

* [NATS](https://nats.io/)
* [NATS server](https://github.com/nats-io/nats-server)
* [NATS Java Client](https://github.com/nats-io/nats.java)
* [Spring Cloud Stream](https://spring.io/projects/spring-cloud-stream)

[License-Url]: https://www.apache.org/licenses/LICENSE-2.0
[License-Image]: https://img.shields.io/badge/License-Apache2-blue.svg

## License <a name="license"></a>

Unless otherwise noted, the nats-account-server source files are distributed under the Apache Version 2.0 license found in the LICENSE file.
