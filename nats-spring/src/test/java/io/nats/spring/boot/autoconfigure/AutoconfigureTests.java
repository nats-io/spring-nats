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

import berlin.yuna.natsserver.config.NatsOptionsBuilder;
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
import java.time.Duration;

import static berlin.yuna.natsserver.config.NatsConfig.NET;
import static berlin.yuna.natsserver.config.NatsConfig.PID;
import static berlin.yuna.natsserver.config.NatsOptions.natsBuilder;
import static berlin.yuna.natsserver.logic.NatsUtils.getNextFreePort;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@ResourceLock(Resources.SYSTEM_PROPERTIES)
class AutoconfigureTests {
    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(NatsAutoConfiguration.class));

    @Test
    void testDefaultConnection() throws IOException, InterruptedException {
        try (var ts = server().nats()) {
            this.contextRunner.withPropertyValues("nats.spring.server=" + ts.url(),
                    "nats.spring.connectionTimeout=15s").run(context -> {
                Connection conn = context.getBean(Connection.class);
                assertThat(conn).isNotNull();
                assertThat(conn.getStatus()).isSameAs(Connection.Status.CONNECTED);
                assertThat(conn.getConnectedUrl()).isEqualTo(ts.url());
                assertThat(conn.getOptions().getConnectionTimeout()).isEqualTo(Duration.ofSeconds(15));
            });
        }
    }

    @Test
    void connectionCanPublishAndSubscribeWithRealServer() throws IOException, InterruptedException {
        try (var ts = server().nats()) {
            this.contextRunner.withPropertyValues("nats.spring.server=" + ts.url()).run(context -> {
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
    void springContextDoesNotCreateConnectionWithoutServerProperties() {
        this.contextRunner.run(context -> assertThat(context).doesNotHaveBean(Connection.class));
    }

    @Test
    void springContextDoesNotCreateConnectionWithBlankServerProperty() {
        this.contextRunner.withPropertyValues("nats.spring.server= ").run(context ->
                assertThat(context).doesNotHaveBean(Connection.class));
    }

    @Test
    void directConnectionFactoryUsesProgrammaticProperties() throws Exception {
        try (var ts = server().nats()) {
            NatsProperties properties = new NatsProperties();
            properties.setServer(ts.url());
            properties.setConnectionTimeout(Duration.ofSeconds(15));
            NatsAutoConfiguration configuration = new NatsAutoConfiguration();

            try (Connection conn = configuration.natsConnection(
                    properties,
                    configuration.defaultConnectionListener(),
                    configuration.defaultErrorListener())) {
                assertThat(conn).isNotNull();
                assertThat(conn.getStatus()).isSameAs(Connection.Status.CONNECTED);
                assertThat(conn.getConnectedUrl()).isEqualTo(ts.url());
                assertThat(conn.getOptions().getConnectionTimeout()).isEqualTo(Duration.ofSeconds(15));
            }
        }
    }

    @Test
    void connectionCanUseTokenAuthWithRealServer() throws IOException, InterruptedException {
        try (var ts = server().customArgs("--auth", "secret").nats()) {
            this.contextRunner.withPropertyValues(
                    "nats.spring.server=" + ts.url(),
                    "nats.spring.token=secret").run(context -> {
                Connection conn = context.getBean(Connection.class);
                assertThat(conn.getStatus()).isSameAs(Connection.Status.CONNECTED);
                assertThat(conn.getConnectedUrl()).isEqualTo(ts.url());
            });
        }
    }

    @Test
    void connectionCanUseUserPasswordAuthWithRealServer() throws IOException, InterruptedException {
        try (var ts = server().customArgs("--user", "spring", "--pass", "nats").nats()) {
            this.contextRunner.withPropertyValues(
                    "nats.spring.server=" + ts.url(),
                    "nats.spring.username=spring",
                    "nats.spring.password=nats").run(context -> {
                Connection conn = context.getBean(Connection.class);
                assertThat(conn.getStatus()).isSameAs(Connection.Status.CONNECTED);
                assertThat(conn.getConnectedUrl()).isEqualTo(ts.url());
            });
        }
    }

    @Test
    void noEchoPreventsConnectionFromReceivingItsOwnPublish() throws IOException, InterruptedException {
        try (var ts = server().nats()) {
            this.contextRunner.withPropertyValues(
                    "nats.spring.server=" + ts.url(),
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
        try (var ts = server().nats()) {
            this.contextRunner.withPropertyValues(
                    "nats.spring.server=" + ts.url(),
                    "nats.spring.no-no-responders=true").run(context -> {
                Connection conn = context.getBean(Connection.class);

                assertThat(conn.getStatus()).isSameAs(Connection.Status.CONNECTED);
                assertThat(conn.getOptions().isNoNoResponders()).isTrue();
            });
        }
    }

    @Test
    void utf8SubjectsCanRoundTripThroughRealServer() throws IOException, InterruptedException {
        try (var ts = server().nats()) {
            this.contextRunner.withPropertyValues(
                    "nats.spring.server=" + ts.url(),
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
        try (var ts = server().customArgs("--tls",
                "--tlscert", "src/test/resources/certs/server-cert.pem",
                "--tlskey", "src/test/resources/certs/server-key.pem",
                "--tlscacert", "src/test/resources/certs/ca.pem").nats()) {
            this.contextRunner.withPropertyValues("nats.spring.server=" + ts.url(),
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
                assertThat(conn.getConnectedUrl()).isEqualTo(ts.url());
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
    void testNoServer() {
        int unusedPort = getNextFreePort(4222);
        this.contextRunner.withPropertyValues(
                "nats.spring.server=nats://127.0.0.1:" + unusedPort,
                "nats.spring.connectionTimeout=250ms").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IOException.class);
        });
    }

    private static NatsOptionsBuilder server() {
        return natsBuilder().port(-1).config(NET, "localhost").config(PID, "target/nats-%PORT%.pid");
    }
}
