/*
 * Copyright 2017-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.nats.spring.boot.autoconfigure;

import io.nats.client.Connection;
import io.nats.client.ConnectionListener;
import io.nats.client.ErrorListener;
import io.nats.client.Message;
import io.nats.client.Subscription;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ResourceLock(Resources.SYSTEM_PROPERTIES)
class AutoconfigureTests {
    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(NatsAutoConfiguration.class));

    @Test
    void testDefaultConnection() throws IOException, InterruptedException {
        try (NatsTestServer ts = new NatsTestServer()) {
            this.contextRunner.withPropertyValues("nats.spring.server=" + ts.getURI(),
                    "nats.spring.connectionTimeout=15s").run(context -> {
                Connection conn = context.getBean(Connection.class);
                assertThat(conn).isNotNull();
                assertThat(conn.getStatus()).isSameAs(Connection.Status.CONNECTED);
                assertThat(conn.getConnectedUrl()).isEqualTo(ts.getURI());
                assertThat(conn.getOptions().getConnectionTimeout()).isEqualTo(Duration.ofSeconds(15));
            });
        }
    }

    @Test
    void connectionCanPublishAndSubscribeWithRealServer() throws IOException, InterruptedException {
        try (NatsTestServer ts = new NatsTestServer()) {
            this.contextRunner.withPropertyValues("nats.spring.server=" + ts.getURI()).run(context -> {
                Connection conn = context.getBean(Connection.class);
                String subject = "spring.autoconfig.e2e";
                String payload = "hello spring nats";
                Subscription sub = conn.subscribe(subject);
                conn.flush(Duration.ofSeconds(5));

                conn.publish(subject, payload.getBytes(UTF_8));
                conn.flush(Duration.ofSeconds(5));

                Message msg = sub.nextMessage(Duration.ofSeconds(5));
                assertThat(msg).isNotNull();
                assertThat(new String(msg.getData(), UTF_8)).isEqualTo(payload);
            });
        }
    }

    @Test
    void directConnectionFactoryUsesProgrammaticProperties() throws Exception {
        try (NatsTestServer ts = new NatsTestServer()) {
            NatsProperties properties = new NatsProperties();
            properties.setServer(ts.getURI());
            properties.setConnectionTimeout(Duration.ofSeconds(15));
            NatsAutoConfiguration configuration = new NatsAutoConfiguration();

            try (Connection conn = configuration.natsConnection(
                    properties,
                    configuration.defaultConnectionListener(),
                    configuration.defaultErrorListener())) {
                assertThat(conn).isNotNull();
                assertThat(conn.getStatus()).isSameAs(Connection.Status.CONNECTED);
                assertThat(conn.getConnectedUrl()).isEqualTo(ts.getURI());
                assertThat(conn.getOptions().getConnectionTimeout()).isEqualTo(Duration.ofSeconds(15));
            }
        }
    }

    @Test
    void connectionCanUseTokenAuthWithRealServer() throws IOException, InterruptedException {
        try (NatsTestServer ts = new NatsTestServer(new String[]{"--auth", "secret"}, false)) {
            this.contextRunner.withPropertyValues(
                    "nats.spring.server=" + ts.getURI(),
                    "nats.spring.token=secret").run(context -> {
                Connection conn = context.getBean(Connection.class);
                assertThat(conn.getStatus()).isSameAs(Connection.Status.CONNECTED);
                assertThat(conn.getConnectedUrl()).isEqualTo(ts.getURI());
            });
        }
    }

    @Test
    void connectionCanUseUserPasswordAuthWithRealServer() throws IOException, InterruptedException {
        try (NatsTestServer ts = new NatsTestServer(new String[]{"--user", "spring", "--pass", "nats"}, false)) {
            this.contextRunner.withPropertyValues(
                    "nats.spring.server=" + ts.getURI(),
                    "nats.spring.username=spring",
                    "nats.spring.password=nats").run(context -> {
                Connection conn = context.getBean(Connection.class);
                assertThat(conn.getStatus()).isSameAs(Connection.Status.CONNECTED);
                assertThat(conn.getConnectedUrl()).isEqualTo(ts.getURI());
            });
        }
    }

    @Test
    void noEchoPreventsConnectionFromReceivingItsOwnPublish() throws IOException, InterruptedException {
        try (NatsTestServer ts = new NatsTestServer()) {
            this.contextRunner.withPropertyValues(
                    "nats.spring.server=" + ts.getURI(),
                    "nats.spring.noEcho=true").run(context -> {
                Connection conn = context.getBean(Connection.class);
                Subscription sub = conn.subscribe("spring.noecho");
                conn.flush(Duration.ofSeconds(5));

                conn.publish("spring.noecho", "hidden from self".getBytes(UTF_8));
                conn.flush(Duration.ofSeconds(5));

                assertThat(sub.nextMessage(Duration.ofMillis(250))).isNull();
            });
        }
    }

    @Test
    void noNoRespondersBindsToRealConnectionOptions() throws IOException, InterruptedException {
        try (NatsTestServer ts = new NatsTestServer()) {
            this.contextRunner.withPropertyValues(
                    "nats.spring.server=" + ts.getURI(),
                    "nats.spring.no-no-responders=true").run(context -> {
                Connection conn = context.getBean(Connection.class);

                assertThat(conn.getStatus()).isSameAs(Connection.Status.CONNECTED);
                assertThat(conn.getOptions().isNoNoResponders()).isTrue();
            });
        }
    }

    @Test
    void utf8SubjectsCanRoundTripThroughRealServer() throws IOException, InterruptedException {
        try (NatsTestServer ts = new NatsTestServer()) {
            this.contextRunner.withPropertyValues(
                    "nats.spring.server=" + ts.getURI(),
                    "nats.spring.utf8Support=true").run(context -> {
                Connection conn = context.getBean(Connection.class);
                String subject = "spring.über";
                String payload = "utf8 subject payload";
                Subscription sub = conn.subscribe(subject);
                conn.flush(Duration.ofSeconds(5));

                conn.publish(subject, payload.getBytes(UTF_8));
                conn.flush(Duration.ofSeconds(5));

                Message msg = sub.nextMessage(Duration.ofSeconds(5));
                assertThat(msg).isNotNull();
                assertThat(new String(msg.getData(), UTF_8)).isEqualTo(payload);
            });
        }
    }

    @Test
    void testSSLConnection() throws IOException, InterruptedException {
        try (NatsTestServer ts = new NatsTestServer("src/test/resources/tls.conf", false)) {
            this.contextRunner.withPropertyValues("nats.spring.server=" + ts.getURI(),
                    "nats.spring.connectionTimeout=15s",
                    "nats.spring.keystorepath=src/test/resources/keystore.jks",
                    "nats.spring.keystorepassword=password",
                    "nats.spring.keystoretype=JKS",
                    "nats.spring.truststorepath=src/test/resources/cacerts",
                    "nats.spring.truststorepassword=password",
                    "nats.spring.truststoretype=JKS",
                    "nats.spring.tlsProtocol=TLSv1.2").run(context -> {
                Connection conn = context.getBean(Connection.class);
                assertThat(conn).isNotNull();
                assertThat(conn.getStatus()).isSameAs(Connection.Status.CONNECTED);
                assertThat(conn.getConnectedUrl()).isEqualTo(ts.getURI());
                assertThat(conn.getOptions().getConnectionTimeout()).isEqualTo(Duration.ofSeconds(15));
            });
        }
    }

    @Test
    void defaultListenersHandleCallbacks() {
        this.contextRunner.run(context -> {
            ConnectionListener connectionListener = context.getBean(ConnectionListener.class);
            ErrorListener errorListener = context.getBean(ErrorListener.class);

            assertThatCode(() -> connectionListener.connectionEvent(null, ConnectionListener.Events.CONNECTED))
                    .doesNotThrowAnyException();
            assertThatCode(() -> errorListener.slowConsumerDetected(null, null))
                    .doesNotThrowAnyException();
            assertThatCode(() -> errorListener.exceptionOccurred(null, new RuntimeException("boom")))
                    .doesNotThrowAnyException();
            assertThatCode(() -> errorListener.errorOccurred(null, "boom"))
                    .doesNotThrowAnyException();
        });
    }

    @Test
    void directConnectionFactoryReturnsNullWithoutServerProperties() throws Exception {
        NatsAutoConfiguration config = new NatsAutoConfiguration();

        assertThat(config.natsConnection(null, null, null)).isNull();
        assertThat(config.natsConnection(new NatsProperties(), null, null)).isNull();
    }

    @Test
    void testServerStartFailsWhenPortIsOwnedByNonNatsListener() throws IOException {
        try (ServerSocket listener = new ServerSocket(0)) {
            assertThatThrownBy(() -> new NatsTestServer(listener.getLocalPort(), false))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("nats-server");
        }
    }

    @Test
    void configBackedServerStartFailureDeletesGeneratedConfig() throws IOException {
        String previousServerPath = System.getProperty("nats_server_path");
        Set<Path> before = tempNatsConfigs();
        System.setProperty("nats_server_path", missingExecutablePath());

        try {
            assertThatThrownBy(() -> new NatsTestServer("src/test/resources/tls.conf", false))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Failed to run");
            assertThat(tempNatsConfigs()).isEqualTo(before);
        } finally {
            restoreServerPath(previousServerPath);
        }
    }

    @Test
    void testNoServer() {
        int unusedPort = NatsTestServer.nextPort();
        this.contextRunner.withPropertyValues(
                "nats.spring.server=nats://127.0.0.1:" + unusedPort,
                "nats.spring.connectionTimeout=250ms").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IOException.class);
        });
    }

    private static Set<Path> tempNatsConfigs() throws IOException {
        Path tempDirectory = Path.of(System.getProperty("java.io.tmpdir"));
        try (Stream<Path> files = Files.list(tempDirectory)) {
            return files
                    .filter(path -> path.getFileName().toString().startsWith("spring_nats_test"))
                    .filter(path -> path.getFileName().toString().endsWith(".conf"))
                    .collect(Collectors.toSet());
        }
    }

    private static String missingExecutablePath() {
        return Path.of(System.getProperty("java.io.tmpdir"), "missing-nats-server-" + System.nanoTime()).toString();
    }

    private static void restoreServerPath(String previousServerPath) {
        if (previousServerPath == null) {
            System.clearProperty("nats_server_path");
        } else {
            System.setProperty("nats_server_path", previousServerPath);
        }
    }
}
