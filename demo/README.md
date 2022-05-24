# NATS Streaming Binder Example

This is a simple example using the NATS Streaming binder. The example lives independently of the other samples, with
only the SpringBoot parent pom.

The example is similar to the processor examples from the nats-samples folder, but shows how to include the binder
independently.

To run:

```bash
% cd demo
% ../mvnw clean package
% java -jar target/demo-0.0.1-SNAPSHOT.jar
```
