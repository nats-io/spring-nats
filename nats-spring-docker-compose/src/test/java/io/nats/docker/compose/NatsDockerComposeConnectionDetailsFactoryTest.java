package io.nats.docker.compose;

import io.nats.spring.boot.autoconfigure.NatsConnectionDetails;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.LinkedHashMap;

import static org.assertj.core.api.Assertions.assertThat;

class NatsDockerComposeConnectionDetailsFactoryTest {

    private final Resource dockerComposeResource = new ClassPathResource("docker-compose.yaml");

    @AfterAll
    static void down() {
        var shutdownHandlers = SpringApplication.getShutdownHandlers();
        ((Runnable) shutdownHandlers).run();
    }

    @Test
    void shouldAccessNatsServerWhenRun() throws IOException {
        var application = new SpringApplication(TestConfig.class);
        var properties = new LinkedHashMap<String, Object>();
        properties.put("spring.docker.compose.skip.in-tests", "false");
        properties.put("spring.docker.compose.file", dockerComposeResource.getFile());
        properties.put("spring.docker.compose.stop.command", "down");
        application.setDefaultProperties(properties);

        var connectionDetails = application.run().getBean(NatsConnectionDetails.class);

        assertThat(connectionDetails.getServer()).satisfiesAnyOf(
                endpoint -> assertThat(endpoint).isEqualTo("nats://localhost:4222"),
                endpoint -> assertThat(endpoint).isEqualTo("nats://127.0.0.1:4222"));
    }

    @Configuration(proxyBeanMethods = false)
    static class TestConfig {
    }
}
