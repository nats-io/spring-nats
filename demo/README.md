# NATS Spring Cloud Stream Binder Demo

Standalone Spring Boot demo for the NATS Spring Cloud Stream binder.

It uses the Spring Cloud Stream functional model with a `Function` bean named `transform`.

Requires Java 17 and runs on Spring Boot 3.5.x with Spring Cloud Stream 4.2.x.

To run:

```bash
% cd demo
% ../mvnw -f ../pom.xml -pl nats-spring-cloud-stream-binder -am -DskipTests -Dgpg.skip install
% ../mvnw clean package
% java -jar target/demo-0.0.1-SNAPSHOT.jar
```
