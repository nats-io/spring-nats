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

package io.nats.cloud.stream.binder;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import io.nats.client.Connection;
import io.nats.client.ConnectionListener;
import io.nats.client.Consumer;
import io.nats.client.ErrorListener;
import io.nats.client.JetStream;
import io.nats.client.JetStreamApiException;
import io.nats.client.JetStreamManagement;
import io.nats.client.JetStreamSubscription;
import io.nats.client.Message;
import io.nats.client.PullSubscribeOptions;
import io.nats.client.Subscription;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.ConsumerInfo;
import io.nats.client.api.StorageType;
import io.nats.client.api.StreamConfiguration;
import io.nats.client.impl.Headers;
import io.nats.cloud.stream.binder.properties.NatsBindingProperties;
import io.nats.cloud.stream.binder.properties.NatsBinderConfigurationProperties;
import io.nats.cloud.stream.binder.properties.NatsConsumerProperties;
import io.nats.cloud.stream.binder.properties.NatsExtendedBindingProperties;
import io.nats.cloud.stream.binder.properties.NatsProducerProperties;
import io.nats.spring.boot.autoconfigure.NatsAutoConfiguration;
import io.nats.spring.boot.autoconfigure.NatsProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.cloud.stream.binder.BinderHeaders;
import org.springframework.cloud.stream.binder.BinderException;
import org.springframework.cloud.stream.binder.Binding;
import org.springframework.cloud.stream.binder.DefaultPollableMessageSource;
import org.springframework.cloud.stream.binder.EmbeddedHeaderUtils;
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.cloud.stream.binder.ExtendedProducerProperties;
import org.springframework.cloud.stream.binder.HeaderMode;
import org.springframework.cloud.stream.binder.MessageValues;
import org.springframework.cloud.stream.binder.RequeueCurrentMessageException;
import org.springframework.cloud.stream.provisioning.ConsumerDestination;
import org.springframework.cloud.stream.provisioning.ProducerDestination;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.acks.AcknowledgmentCallback;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.messaging.support.MessageBuilder;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

@ResourceLock(Resources.SYSTEM_PROPERTIES)
class BinderTests {
    private static final Duration FLUSH_TIMEOUT = Duration.ofSeconds(5);
    private static final String SHARED_TLS_RESOURCES = "../nats-spring/src/test/resources/";
    private static final String TRACE_HEADER = "x-trace-id";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(NatsAutoConfiguration.class));
    private final ApplicationContextRunner binderContextRunner = new ApplicationContextRunner()
            .withUserConfiguration(NatsChannelBinderConfiguration.class);

    @Test
    void createBinderFromGlobalProperties() throws IOException, InterruptedException {
        try (NatsBinderTestServer ts = new NatsBinderTestServer()) {
            this.contextRunner.run(context -> {
                try (BinderFixture fixture = newGlobalBinder(ts.getURI())) {
                    assertConnected(fixture.connection(), ts.getURI());
                    assertDefaultListenersCanHandleCallbacks(fixture.connection());
                    assertThat(fixture.binder().getDefaultsPrefix()).isEqualTo("nats.spring.cloud.stream.default");
                    assertThat(fixture.binder().getExtendedPropertiesEntryClass()).isEqualTo(NatsBindingProperties.class);
                    assertThat(fixture.binder().getExtendedConsumerProperties("input")).isNotNull();
                    assertThat(fixture.binder().getExtendedProducerProperties("output")).isNotNull();
                }
            });
        }
    }

    @Test
    void createBinderFromBinderProperties() throws IOException, InterruptedException {
        try (NatsBinderTestServer ts = new NatsBinderTestServer()) {
            this.contextRunner.run(context -> {
                try (BinderFixture fixture = newBinderPropertiesBinder(ts.getURI())) {
                    assertConnected(fixture.connection(), ts.getURI());
                }
            });
        }
    }

    @Test
    void createBinderWithoutServerPropertiesFailsExplicitly() {
        NatsChannelBinderConfiguration config = new NatsChannelBinderConfiguration(
                null,
                null,
                new NatsProperties(),
                new NatsBinderConfigurationProperties(),
                new NatsExtendedBindingProperties());
        NatsChannelProvisioner provisioner = config.natsChannelProvisioner();

        assertThatThrownBy(() -> config.natsBinder(provisioner))
                .isInstanceOf(IOException.class)
                .hasMessage("NATS binder could not establish a connection");
    }

    @Test
    void createBinderWithoutConfigurationObjectsLeavesConnectionUnset() {
        NatsChannelBinder binder = new NatsChannelBinder(
                new NatsExtendedBindingProperties(),
                null,
                null,
                new NatsChannelProvisioner(),
                null,
                null);

        assertThat(binder.getConnection()).isNull();
    }

    @Test
    void createBinderRejectsMissingRequiredCollaborators() {
        assertThatThrownBy(() -> new NatsChannelBinder(
                null,
                null,
                null,
                new NatsChannelProvisioner(),
                null,
                null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("bindingProperties must not be null");

        assertThatThrownBy(() -> new NatsChannelBinder(
                new NatsExtendedBindingProperties(),
                null,
                null,
                null,
                null,
                null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("provisioningProvider must not be null");
    }

    @Test
    void createBinderWithBlankServerPropertiesLeavesConnectionUnset() {
        NatsBinderConfigurationProperties binderProps = new NatsBinderConfigurationProperties();
        binderProps.setServer("");
        NatsProperties natsProperties = new NatsProperties();
        natsProperties.setServer("");
        NatsChannelBinder binder = new NatsChannelBinder(
                new NatsExtendedBindingProperties(),
                binderProps,
                natsProperties,
                new NatsChannelProvisioner(),
                null,
                null);

        assertThat(binder.getConnection()).isNull();
    }

    @Test
    void binderConfigurationExposesAccessorsAndDefaultMappings() {
        NatsBinderConfigurationProperties binderProps = new NatsBinderConfigurationProperties();
        NatsExtendedBindingProperties extendedProps = new NatsExtendedBindingProperties();
        NatsProperties natsProperties = new NatsProperties();
        NatsChannelBinderConfiguration config = new NatsChannelBinderConfiguration(
                null,
                null,
                natsProperties,
                binderProps,
                extendedProps);

        ConfigurationPropertyName streamPrefix = ConfigurationPropertyName.of("nats.spring.cloud.stream");
        Map<ConfigurationPropertyName, ConfigurationPropertyName> mappings =
                config.natsExtendedPropertiesDefaultMappingsProvider().getDefaultMappings();

        assertThat(config.getNatsBinderConfigurationProperties()).isSameAs(binderProps);
        assertThat(config.getNatsExtendedBindingProperties()).isSameAs(extendedProps);
        assertThat(config.getNatsProperties()).isSameAs(natsProperties);
        assertThat(config.natsChannelProvisioner()).isNotNull();
        assertThat(mappings).containsEntry(streamPrefix, streamPrefix);
    }

    @Test
    void binderConfigurationRejectsMissingRequiredPropertyObjects() {
        assertThatThrownBy(() -> new NatsChannelBinderConfiguration(
                null,
                null,
                null,
                new NatsBinderConfigurationProperties(),
                new NatsExtendedBindingProperties()))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("natsProperties must not be null");

        assertThatThrownBy(() -> new NatsChannelBinderConfiguration(
                null,
                null,
                new NatsProperties(),
                null,
                new NatsExtendedBindingProperties()))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("natsBinderConfigurationProperties must not be null");

        assertThatThrownBy(() -> new NatsChannelBinderConfiguration(
                null,
                null,
                new NatsProperties(),
                new NatsBinderConfigurationProperties(),
                null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("natsExtendedBindingProperties must not be null");
    }

    @Test
    void binderConfigurationBindsCustomEmbeddedHeaders() {
        this.binderContextRunner
                .withPropertyValues("nats.spring.cloud.stream.binder.headers-to-embed=" + TRACE_HEADER + ",x-tenant-id")
                .run(context -> {
                    NatsBinderConfigurationProperties properties =
                            context.getBean(NatsBinderConfigurationProperties.class);

                    assertThat(properties.getHeadersToEmbed()).containsExactly(TRACE_HEADER, "x-tenant-id");
                });
    }

    @Test
    void binderConfigurationBindsJetStreamExtendedPropertiesForIssue52() {
        this.binderContextRunner
                .withPropertyValues(
                        "nats.spring.cloud.stream.bindings.input.consumer.jet-stream=true",
                        "nats.spring.cloud.stream.bindings.input.consumer.stream-name=ORDERS",
                        "nats.spring.cloud.stream.bindings.input.consumer.consumer-name=orders-worker",
                        "nats.spring.cloud.stream.bindings.output.producer.jet-stream=true",
                        "nats.spring.cloud.stream.bindings.output.producer.stream-name=ORDERS",
                        "nats.spring.cloud.stream.bindings.output.producer.provision-stream=true",
                        "nats.spring.cloud.stream.bindings.output.producer.stream-storage-type=memory",
                        "nats.spring.cloud.stream.bindings.output.producer.stream-replicas=1")
                .run(context -> {
                    NatsExtendedBindingProperties properties = context.getBean(NatsExtendedBindingProperties.class);

                    NatsConsumerProperties consumer = properties.getExtendedConsumerProperties("input");
                    assertThat(consumer.isJetStream()).isTrue();
                    assertThat(consumer.getStreamName()).isEqualTo("ORDERS");
                    assertThat(consumer.getConsumerName()).isEqualTo("orders-worker");

                    NatsProducerProperties producer = properties.getExtendedProducerProperties("output");
                    assertThat(producer.isJetStream()).isTrue();
                    assertThat(producer.getStreamName()).isEqualTo("ORDERS");
                    assertThat(producer.isProvisionStream()).isTrue();
                    assertThat(producer.getStreamStorageType()).isEqualTo(StorageType.Memory);
                    assertThat(producer.getStreamReplicas()).isEqualTo(1);
                });
    }

    @Test
    void testServerStartFailsWhenPortIsOwnedByNonNatsListener() throws IOException {
        try (ServerSocket listener = new ServerSocket(0)) {
            assertThatThrownBy(() -> new NatsBinderTestServer(listener.getLocalPort(), false))
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
            assertThatThrownBy(() -> new NatsBinderTestServer(SHARED_TLS_RESOURCES + "tls.conf", false))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Failed to run");
            assertThat(tempNatsConfigs()).isEqualTo(before);
        } finally {
            restoreServerPath(previousServerPath);
        }
    }

    @Test
    void createBinderFailsExplicitlyWhenAuthenticationFails() throws IOException, InterruptedException {
        try (NatsBinderTestServer ts = new NatsBinderTestServer(new String[]{"--auth", "secret"}, false)) {
            NatsBinderConfigurationProperties binderProps = new NatsBinderConfigurationProperties();
            binderProps.setServer(ts.getURI());
            binderProps.setConnectionTimeout(Duration.ofSeconds(1));
            NatsChannelBinderConfiguration config = new NatsChannelBinderConfiguration(
                    null,
                    null,
                    new NatsProperties(),
                    binderProps,
                    new NatsExtendedBindingProperties());
            NatsChannelProvisioner provisioner = config.natsChannelProvisioner();

            assertThatThrownBy(() -> config.natsBinder(provisioner))
                    .isInstanceOf(IOException.class)
                    .hasMessage("NATS binder could not establish a connection");
        }
    }

    @Test
    void createBinderFailsExplicitlyWhenServerIsUnreachable() {
        int unusedPort = NatsBinderTestServer.nextPort();
        NatsBinderConfigurationProperties binderProps = new NatsBinderConfigurationProperties();
        binderProps.setServer("nats://127.0.0.1:" + unusedPort);
        binderProps.setConnectionTimeout(Duration.ofMillis(250));
        NatsChannelBinderConfiguration config = new NatsChannelBinderConfiguration(
                null,
                null,
                new NatsProperties(),
                binderProps,
                new NatsExtendedBindingProperties());
        NatsChannelProvisioner provisioner = config.natsChannelProvisioner();

        assertThatThrownBy(() -> config.natsBinder(provisioner))
                .isInstanceOf(IOException.class)
                .hasMessage("NATS binder could not establish a connection");
    }

    @Test
    void createTLSBinderFromGlobalProperties() throws IOException, InterruptedException {
        try (NatsBinderTestServer ts = new NatsBinderTestServer(SHARED_TLS_RESOURCES + "tls.conf", false)) {
            this.contextRunner.run(context -> {
                try (BinderFixture fixture = newTlsGlobalBinder(ts.getURI())) {
                    assertConnected(fixture.connection(), ts.getURI());
                }
            });
        }
    }

    @Test
    void createBinderWithCustomListeners() throws IOException, InterruptedException {
        try (NatsBinderTestServer ts = new NatsBinderTestServer()) {
            AtomicReference<ConnectionListener.Events> event = new AtomicReference<>();
            NatsChannelBinder binder = new NatsChannelBinder(
                    new NatsExtendedBindingProperties(),
                    new NatsBinderConfigurationProperties(),
                    (NatsProperties) new NatsProperties().server(ts.getURI()),
                    new NatsChannelProvisioner(),
                    (connection, type) -> event.set(type),
                    new ErrorListener() {
                        @Override
                        public void slowConsumerDetected(Connection conn, Consumer consumer) {
                        }

                        @Override
                        public void exceptionOccurred(Connection conn, Exception exp) {
                        }

                        @Override
                        public void errorOccurred(Connection conn, String error) {
                        }
                    });

            try {
                assertConnected(binder.getConnection(), ts.getURI());
                await().atMost(5, TimeUnit.SECONDS)
                        .untilAsserted(() -> assertThat(event.get()).isNotNull());
            } finally {
                binder.getConnection().close();
            }
        }
    }

    @Test
    void springContextCreatesBinderFromBinderProperties() throws IOException, InterruptedException {
        try (NatsBinderTestServer ts = new NatsBinderTestServer()) {
            this.binderContextRunner.withPropertyValues(
                    "nats.spring.cloud.stream.binder.server=" + ts.getURI()).run(context -> {
                assertThat(context).hasSingleBean(NatsChannelBinder.class);
                NatsChannelBinder binder = context.getBean(NatsChannelBinder.class);
                assertConnected(binder.getConnection(), ts.getURI());
                binder.getConnection().close();
            });
        }
    }

    @Test
    void springContextCreatesBinderFromGlobalProperties() throws IOException, InterruptedException {
        try (NatsBinderTestServer ts = new NatsBinderTestServer()) {
            this.binderContextRunner.withPropertyValues("nats.spring.server=" + ts.getURI()).run(context -> {
                assertThat(context).hasSingleBean(NatsChannelBinder.class);
                assertConnected(context.getBean(Connection.class), ts.getURI());

                NatsChannelBinder binder = context.getBean(NatsChannelBinder.class);
                assertConnected(binder.getConnection(), ts.getURI());
                binder.getConnection().close();
            });
        }
    }

    @Test
    void springContextDoesNotCreateBinderWithoutServerProperties() {
        this.binderContextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(Connection.class);
            assertThat(context).doesNotHaveBean(NatsChannelBinder.class);
        });
    }

    @Test
    void springContextDoesNotCreateBinderWithBlankServerProperties() {
        this.binderContextRunner.withPropertyValues(
                "nats.spring.server= ",
                "nats.spring.cloud.stream.binder.server= ").run(context -> {
            assertThat(context).doesNotHaveBean(Connection.class);
            assertThat(context).doesNotHaveBean(NatsChannelBinder.class);
        });
    }

    @Test
    void springContextFailsExplicitlyWhenBinderServerIsUnreachable() {
        int unusedPort = NatsBinderTestServer.nextPort();
        this.binderContextRunner.withPropertyValues(
                "nats.spring.cloud.stream.binder.server=nats://127.0.0.1:" + unusedPort,
                "nats.spring.cloud.stream.binder.connectionTimeout=250ms").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IOException.class);
        });
    }

    @Test
    void binderPropertiesWinOverGlobalProperties() throws IOException, InterruptedException {
        try (NatsBinderTestServer global = new NatsBinderTestServer();
             NatsBinderTestServer binderSpecific = new NatsBinderTestServer()) {
            this.binderContextRunner.withPropertyValues(
                    "nats.spring.server=" + global.getURI(),
                    "nats.spring.cloud.stream.binder.server=" + binderSpecific.getURI()).run(context -> {
                Connection globalConnection = context.getBean(Connection.class);
                NatsChannelBinder binder = context.getBean(NatsChannelBinder.class);
                try {
                    assertConnected(globalConnection, global.getURI());
                    assertConnected(binder.getConnection(), binderSpecific.getURI());
                } finally {
                    globalConnection.close();
                    binder.getConnection().close();
                }
            });
        }
    }

    @Test
    void testMessageProducer() throws Exception {
        try (NatsBinderTestServer ts = new NatsBinderTestServer()) {
            this.contextRunner.withPropertyValues("nats.spring.server=" + ts.getURI()).run(context -> {
                Connection conn = context.getBean(Connection.class);
                assertConnected(conn, ts.getURI());

                try (BinderFixture fixture = newGlobalBinder(ts.getURI())) {
                    String theMessage = "hello world";
                    String in = "in";

                    ConsumerDestination from = fixture.provisioner().provisionConsumerDestination(in, "", null);
                    NatsMessageProducer producer = (NatsMessageProducer) fixture.binder().createConsumerEndpoint(from, "", null);
                    CompletableFuture<String> received = new CompletableFuture<>();
                    DirectChannel output = new DirectChannel();
                    output.subscribe(msg -> received.complete(payloadText(msg.getPayload())));
                    producer.setOutputChannel(output);

                    assertThat(producer.getOutputChannel()).isSameAs(output);
                    assertThat(producer.isRunning()).isFalse();

                    try {
                        producer.start();
                        assertThat(producer.isRunning()).isTrue();
                        fixture.connection().flush(FLUSH_TIMEOUT);

                        conn.publish(in, theMessage.getBytes(UTF_8));
                        conn.flush(FLUSH_TIMEOUT);

                        assertThat(received.get(5, TimeUnit.SECONDS)).isEqualTo(theMessage);
                    } finally {
                        producer.stop();
                    }
                }
            });
        }
    }

    @Test
    void messageProducerStartAndStopAreIdempotent() throws Exception {
        try (NatsBinderTestServer ts = new NatsBinderTestServer()) {
            this.contextRunner.withPropertyValues("nats.spring.server=" + ts.getURI()).run(context -> {
                Connection conn = context.getBean(Connection.class);
                assertConnected(conn, ts.getURI());

                try (BinderFixture fixture = newGlobalBinder(ts.getURI())) {
                    String subject = "producer.lifecycle";
                    ConsumerDestination from = fixture.provisioner().provisionConsumerDestination(subject, "", null);
                    NatsMessageProducer producer =
                            (NatsMessageProducer) fixture.binder().createConsumerEndpoint(from, "", null);
                    CompletableFuture<String> received = new CompletableFuture<>();
                    DirectChannel output = new DirectChannel();
                    output.subscribe(msg -> received.complete(payloadText(msg.getPayload())));
                    producer.setOutputChannel(output);

                    producer.start();
                    producer.start();
                    assertThat(producer.isRunning()).isTrue();
                    fixture.connection().flush(FLUSH_TIMEOUT);

                    conn.publish(subject, "once".getBytes(UTF_8));
                    conn.flush(FLUSH_TIMEOUT);

                    assertThat(received.get(5, TimeUnit.SECONDS)).isEqualTo("once");

                    producer.stop();
                    producer.stop();
                    assertThat(producer.isRunning()).isFalse();
                }
            });
        }
    }

    @Test
    void messageProducerSimpleConstructorReceivesMessage() throws Exception {
        try (NatsBinderTestServer ts = new NatsBinderTestServer()) {
            this.contextRunner.withPropertyValues("nats.spring.server=" + ts.getURI()).run(context -> {
                Connection conn = context.getBean(Connection.class);
                assertConnected(conn, ts.getURI());

                try (BinderFixture fixture = newGlobalBinder(ts.getURI())) {
                    String subject = "producer.simple.constructor";
                    NatsConsumerDestination from =
                            (NatsConsumerDestination) fixture.provisioner().provisionConsumerDestination(subject, "", null);
                    NatsMessageProducer producer = new NatsMessageProducer(from, fixture.connection());
                    CompletableFuture<String> received = new CompletableFuture<>();
                    DirectChannel output = new DirectChannel();
                    output.subscribe(msg -> received.complete(payloadText(msg.getPayload())));
                    producer.setOutputChannel(output);

                    try {
                        producer.start();
                        fixture.connection().flush(FLUSH_TIMEOUT);

                        conn.publish(subject, "simple".getBytes(UTF_8));
                        conn.flush(FLUSH_TIMEOUT);

                        assertThat(received.get(5, TimeUnit.SECONDS)).isEqualTo("simple");
                    } finally {
                        producer.stop();
                    }
                }
            });
        }
    }

    @Test
    void messageProducerWithoutOutputChannelKeepsRunningAndDropsMessage() throws Exception {
        try (NatsBinderTestServer ts = new NatsBinderTestServer()) {
            this.contextRunner.withPropertyValues("nats.spring.server=" + ts.getURI()).run(context -> {
                Connection conn = context.getBean(Connection.class);
                assertConnected(conn, ts.getURI());

                try (BinderFixture fixture = newGlobalBinder(ts.getURI())) {
                    String subject = "producer.no.output";
                    ConsumerDestination from = fixture.provisioner().provisionConsumerDestination(subject, "", null);
                    NatsMessageProducer producer =
                            (NatsMessageProducer) fixture.binder().createConsumerEndpoint(from, "", null);

                    try {
                        producer.start();
                        fixture.connection().flush(FLUSH_TIMEOUT);

                        conn.publish(subject, "dropped".getBytes(UTF_8));
                        conn.flush(FLUSH_TIMEOUT);

                        await().atMost(1, TimeUnit.SECONDS)
                                .untilAsserted(() -> assertThat(producer.isRunning()).isTrue());
                    } finally {
                        producer.stop();
                    }
                }
            });
        }
    }

    @Test
    void messageProducerKeepsRunningWhenOutputChannelThrows() throws Exception {
        try (NatsBinderTestServer ts = new NatsBinderTestServer()) {
            this.contextRunner.withPropertyValues("nats.spring.server=" + ts.getURI()).run(context -> {
                Connection conn = context.getBean(Connection.class);
                assertConnected(conn, ts.getURI());

                try (BinderFixture fixture = newGlobalBinder(ts.getURI())) {
                    String subject = "producer.output.failure";
                    ConsumerDestination from = fixture.provisioner().provisionConsumerDestination(subject, "", null);
                    NatsMessageProducer producer =
                            (NatsMessageProducer) fixture.binder().createConsumerEndpoint(from, "", null);
                    DirectChannel output = new DirectChannel();
                    output.subscribe(msg -> {
                        throw new IllegalStateException("channel failed");
                    });
                    producer.setOutputChannel(output);

                    try {
                        producer.start();
                        fixture.connection().flush(FLUSH_TIMEOUT);

                        conn.publish(subject, "fail safely".getBytes(UTF_8));
                        conn.flush(FLUSH_TIMEOUT);

                        await().atMost(5, TimeUnit.SECONDS)
                                .untilAsserted(() -> assertThat(producer.isRunning()).isTrue());
                    } finally {
                        producer.stop();
                    }
                }
            });
        }
    }

    @Test
    void testMessageProducerWithGroup() throws Exception {
        try (NatsBinderTestServer ts = new NatsBinderTestServer()) {
            this.contextRunner.withPropertyValues("nats.spring.server=" + ts.getURI()).run(context -> {
                Connection conn = context.getBean(Connection.class);
                assertConnected(conn, ts.getURI());

                try (BinderFixture fixture = newGlobalBinder(ts.getURI())) {
                    String theMessage = "hello world";
                    String in = "in";
                    String group = "group";
                    int total = 100;

                    ConsumerDestination from = fixture.provisioner().provisionConsumerDestination(in, group, null);
                    AtomicInteger counter = new AtomicInteger(0);

                    NatsMessageProducer producer = buildCountingProducer(from, group, fixture, counter);
                    NatsMessageProducer producer2 = buildCountingProducer(from, group, fixture, counter);

                    try {
                        producer.start();
                        producer2.start();
                        fixture.connection().flush(FLUSH_TIMEOUT);

                        for (int i = 0; i < total; i++) {
                            conn.publish(in, theMessage.getBytes(UTF_8));
                        }
                        conn.flush(FLUSH_TIMEOUT);

                        await().atMost(5, TimeUnit.SECONDS)
                                .untilAsserted(() -> assertThat(counter.get()).isEqualTo(total));
                    } finally {
                        producer.stop();
                        producer2.stop();
                    }
                }
            });
        }
    }

    @Test
    void messageSourceStartAndStopAreIdempotent() throws Exception {
        try (NatsBinderTestServer ts = new NatsBinderTestServer()) {
            this.contextRunner.withPropertyValues("nats.spring.server=" + ts.getURI()).run(context -> {
                Connection conn = context.getBean(Connection.class);
                assertConnected(conn, ts.getURI());

                try (BinderFixture fixture = newGlobalBinder(ts.getURI())) {
                    String subject = "source.lifecycle";
                    NatsConsumerDestination from =
                            (NatsConsumerDestination) fixture.provisioner().provisionConsumerDestination(subject, "", null);
                    NatsMessageSource src = new NatsMessageSource(from, fixture.connection());

                    assertThat(src.receive()).isNull();
                    src.stop();

                    src.start();
                    src.start();
                    assertThat(src.isRunning()).isTrue();
                    fixture.connection().flush(FLUSH_TIMEOUT);

                    CompletableFuture<org.springframework.messaging.Message<Object>> received =
                            CompletableFuture.supplyAsync(src::receive);

                    conn.publish(subject, "source-once".getBytes(UTF_8));
                    conn.flush(FLUSH_TIMEOUT);

                    assertThat(payloadText(received.get(5, TimeUnit.SECONDS).getPayload())).isEqualTo("source-once");

                    src.stop();
                    src.stop();
                    assertThat(src.isRunning()).isFalse();
                }
            });
        }
    }

    @Test
    void messageSourceReceiveReturnsNullWhenInterrupted() throws Exception {
        try (NatsBinderTestServer ts = new NatsBinderTestServer()) {
            this.contextRunner.withPropertyValues("nats.spring.server=" + ts.getURI()).run(context -> {
                Connection conn = context.getBean(Connection.class);
                assertConnected(conn, ts.getURI());

                try (BinderFixture fixture = newGlobalBinder(ts.getURI())) {
                    NatsConsumerDestination from = (NatsConsumerDestination) fixture.provisioner()
                            .provisionConsumerDestination("source.interrupted", "", null);
                    NatsMessageSource src = new NatsMessageSource(from, fixture.connection());
                    AtomicReference<org.springframework.messaging.Message<Object>> received = new AtomicReference<>();
                    AtomicReference<Throwable> failure = new AtomicReference<>();
                    CountDownLatch started = new CountDownLatch(1);
                    AtomicInteger done = new AtomicInteger(0);

                    try {
                        src.start();
                        fixture.connection().flush(FLUSH_TIMEOUT);
                        assertThat(src.getComponentType()).isEqualTo("nats:message-source");

                        Thread receiver = new Thread(() -> {
                            started.countDown();
                            try {
                                received.set(src.receive());
                            } catch (Throwable exp) {
                                failure.set(exp);
                            } finally {
                                done.incrementAndGet();
                            }
                        });
                        receiver.start();
                        assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
                        receiver.interrupt();

                        await().atMost(5, TimeUnit.SECONDS)
                                .untilAsserted(() -> assertThat(done.get()).isEqualTo(1));
                        assertThat(failure.get()).isNull();
                        assertThat(received.get()).isNull();
                    } finally {
                        src.stop();
                    }
                }
            });
        }
    }

    @Test
    void testMessageSource() throws Exception {
        try (NatsBinderTestServer ts = new NatsBinderTestServer()) {
            this.contextRunner.withPropertyValues("nats.spring.server=" + ts.getURI()).run(context -> {
                Connection conn = context.getBean(Connection.class);
                assertConnected(conn, ts.getURI());

                try (BinderFixture fixture = newGlobalBinder(ts.getURI())) {
                    String theMessage = "hello world";
                    String in = "in";

                    NatsConsumerDestination from =
                            (NatsConsumerDestination) fixture.provisioner().provisionConsumerDestination(in, "", null);
                    NatsMessageSource src = new NatsMessageSource(from, fixture.connection());

                    assertThat(src.isRunning()).isFalse();
                    try {
                        src.start();
                        assertThat(src.isRunning()).isTrue();
                        fixture.connection().flush(FLUSH_TIMEOUT);

                        CompletableFuture<org.springframework.messaging.Message<Object>> received =
                                CompletableFuture.supplyAsync(src::receive);

                        conn.publish(in, theMessage.getBytes(UTF_8));
                        conn.flush(FLUSH_TIMEOUT);

                        assertThat(payloadText(received.get(5, TimeUnit.SECONDS).getPayload())).isEqualTo(theMessage);
                    } finally {
                        src.stop();
                    }
                }
            });
        }
    }

    @Test
    void messageSourcePreservesNatsHeadersAsSpringHeadersForIssue65() throws Exception {
        try (NatsBinderTestServer ts = new NatsBinderTestServer()) {
            this.contextRunner.withPropertyValues("nats.spring.server=" + ts.getURI()).run(context -> {
                Connection conn = context.getBean(Connection.class);
                assertConnected(conn, ts.getURI());

                try (BinderFixture fixture = newGlobalBinder(ts.getURI())) {
                    String subject = "source.headers.issue65";
                    String payload = "headers survive polling";
                    NatsConsumerDestination from =
                            (NatsConsumerDestination) fixture.provisioner().provisionConsumerDestination(subject, "", null);
                    NatsMessageSource src = new NatsMessageSource(from, fixture.connection());

                    try {
                        src.start();
                        fixture.connection().flush(FLUSH_TIMEOUT);

                        conn.publish(subject, headersWithTrace("poll-123"), payload.getBytes(UTF_8));
                        conn.flush(FLUSH_TIMEOUT);

                        org.springframework.messaging.Message<Object> received = src.receive();
                        assertThat(received).isNotNull();
                        assertThat(payloadText(received.getPayload())).isEqualTo(payload);
                        assertThat(received.getHeaders()).containsEntry(TRACE_HEADER, "poll-123");
                    } finally {
                        src.stop();
                    }
                }
            });
        }
    }

    @Test
    void testMessageSourceWithQueue() throws Exception {
        try (NatsBinderTestServer ts = new NatsBinderTestServer()) {
            this.contextRunner.withPropertyValues("nats.spring.server=" + ts.getURI()).run(context -> {
                Connection conn = context.getBean(Connection.class);
                assertConnected(conn, ts.getURI());

                try (BinderFixture fixture = newGlobalBinder(ts.getURI())) {
                    String theMessage = "hello world";
                    String in = "in";
                    String group = "group";

                    NatsConsumerDestination from =
                            (NatsConsumerDestination) fixture.provisioner().provisionConsumerDestination(in, group, null);
                    NatsMessageSource src = new NatsMessageSource(from, fixture.connection());

                    assertThat(src.isRunning()).isFalse();
                    try {
                        src.start();
                        assertThat(src.isRunning()).isTrue();
                        fixture.connection().flush(FLUSH_TIMEOUT);

                        CompletableFuture<org.springframework.messaging.Message<Object>> received =
                                CompletableFuture.supplyAsync(src::receive);

                        conn.publish(in, theMessage.getBytes(UTF_8));
                        conn.flush(FLUSH_TIMEOUT);

                        assertThat(payloadText(received.get(5, TimeUnit.SECONDS).getPayload())).isEqualTo(theMessage);
                    } finally {
                        src.stop();
                    }
                }
            });
        }
    }

    @Test
    void messageHandlerWithoutConnectionDropsSupportedPayload() {
        MessageHandler handler = new NatsMessageHandler("handler.no.connection", null);

        assertThatCode(() -> handler.handleMessage(new GenericMessage<>("ignored"))).doesNotThrowAnyException();
    }

    @Test
    void messageHandlerRejectsNullSubject() {
        assertThatThrownBy(() -> new NatsMessageHandler(null, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("subject must not be null");
    }

    @Test
    void messageProducerWithoutConnectionDoesNotStart() {
        NatsMessageProducer producer = new NatsMessageProducer(new NatsConsumerDestination("producer.no.connection"), null);

        assertThatCode(producer::start).doesNotThrowAnyException();
        assertThat(producer.isRunning()).isFalse();
        assertThatCode(producer::stop).doesNotThrowAnyException();
    }

    @Test
    void messageProducerRejectsNullDestination() {
        assertThatThrownBy(() -> new NatsMessageProducer(null, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("destination must not be null");
    }

    @Test
    void messageSourceWithoutConnectionDoesNotStartOrReceive() {
        NatsMessageSource source = new NatsMessageSource(new NatsConsumerDestination("source.no.connection"), null);

        assertThatCode(source::start).doesNotThrowAnyException();
        assertThat(source.isRunning()).isFalse();
        assertThat(source.receive()).isNull();
        assertThatCode(source::stop).doesNotThrowAnyException();
    }

    @Test
    void messageSourceRejectsNullDestination() {
        assertThatThrownBy(() -> new NatsMessageSource(null, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("destination must not be null");
    }

    @Test
    void testMessageHandler() throws Exception {
        try (NatsBinderTestServer ts = new NatsBinderTestServer()) {
            this.contextRunner.withPropertyValues("nats.spring.server=" + ts.getURI()).run(context -> {
                Connection conn = context.getBean(Connection.class);
                assertConnected(conn, ts.getURI());

                try (BinderFixture fixture = newGlobalBinder(ts.getURI())) {
                    String theMessage = "hello world";
                    String out = "out";
                    ProducerDestination to = fixture.provisioner().provisionProducerDestination(out, null);
                    MessageHandler mh = fixture.binder().createProducerMessageHandler(to, null, null);

                    Subscription sub = conn.subscribe(out);
                    conn.flush(FLUSH_TIMEOUT);

                    mh.handleMessage(new GenericMessage<>(theMessage.getBytes(UTF_8)));
                    fixture.connection().flush(FLUSH_TIMEOUT);
                    assertThat(nextText(sub)).isEqualTo(theMessage);

                    ByteBuffer buffer = ByteBuffer.wrap(theMessage.getBytes(UTF_8));
                    mh.handleMessage(new GenericMessage<>(buffer));
                    fixture.connection().flush(FLUSH_TIMEOUT);
                    assertThat(nextText(sub)).isEqualTo(theMessage);

                    mh.handleMessage(new GenericMessage<>(theMessage));
                    fixture.connection().flush(FLUSH_TIMEOUT);
                    assertThat(nextText(sub)).isEqualTo(theMessage);

                    mh.handleMessage(new GenericMessage<>(Integer.valueOf(2)));
                    fixture.connection().flush(FLUSH_TIMEOUT);
                    assertThat(sub.nextMessage(Duration.ofMillis(250))).isNull();
                }
            });
        }
    }

    @Test
    void messageHandlerPublishesCustomHeadersForIssue65() throws Exception {
        try (NatsBinderTestServer ts = new NatsBinderTestServer()) {
            this.contextRunner.withPropertyValues("nats.spring.server=" + ts.getURI()).run(context -> {
                Connection conn = context.getBean(Connection.class);
                assertConnected(conn, ts.getURI());

                try (BinderFixture fixture = newGlobalBinder(ts.getURI())) {
                    String subject = "handler.headers.issue65";
                    String payload = "headers survive publish";
                    ProducerDestination to = fixture.provisioner().provisionProducerDestination(subject, null);
                    MessageHandler handler = fixture.binder().createProducerMessageHandler(to, null, null);

                    Subscription sub = conn.subscribe(subject);
                    conn.flush(FLUSH_TIMEOUT);

                    handler.handleMessage(MessageBuilder.withPayload(payload)
                            .setHeader(TRACE_HEADER, "send-123")
                            .setHeader("custom-number", 42)
                            .setHeader("custom-list", Arrays.asList("first", null, "second"))
                            .build());
                    fixture.connection().flush(FLUSH_TIMEOUT);

                    Message received = sub.nextMessage(FLUSH_TIMEOUT);
                    assertThat(received).isNotNull();
                    assertThat(new String(received.getData(), UTF_8)).isEqualTo(payload);
                    assertThat(received.hasHeaders()).isTrue();
                    assertThat(received.getHeaders().getFirst(TRACE_HEADER)).isEqualTo("send-123");
                    assertThat(received.getHeaders().getFirst("custom-number")).isEqualTo("42");
                    assertThat(received.getHeaders().get("custom-list")).containsExactly("first", "second");
                }
            });
        }
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void messageHandlerSkipsHeadersThatCannotBeMappedToNatsHeaders(CapturedOutput output) throws Exception {
        Logger mapperLogger = (Logger) LoggerFactory.getLogger("io.nats.cloud.stream.binder.NatsHeaderMapper");
        Level originalLevel = mapperLogger.getLevel();
        mapperLogger.setLevel(Level.DEBUG);
        try (NatsBinderTestServer ts = new NatsBinderTestServer()) {
            this.contextRunner.withPropertyValues("nats.spring.server=" + ts.getURI()).run(context -> {
                Connection conn = context.getBean(Connection.class);
                assertConnected(conn, ts.getURI());

                try (BinderFixture fixture = newGlobalBinder(ts.getURI())) {
                    String subject = "handler.headers.skipped";
                    String payload = "only payload";
                    ProducerDestination to = fixture.provisioner().provisionProducerDestination(subject, null);
                    MessageHandler handler = fixture.binder().createProducerMessageHandler(to, null, null);

                    Subscription sub = conn.subscribe(subject);
                    conn.flush(FLUSH_TIMEOUT);

                    Map<String, Object> headers = new HashMap<>();
                    headers.put("", "blank");
                    headers.put(NatsMessageProducer.SUBJECT, "reserved");
                    headers.put(BinderHeaders.NATIVE_HEADERS_PRESENT, true);
                    headers.put("empty-custom", List.of());
                    headers.put("invalid-value", "bad\r\nvalue");

                    handler.handleMessage(new GenericMessage<>(payload, headers));
                    fixture.connection().flush(FLUSH_TIMEOUT);

                    Message received = sub.nextMessage(FLUSH_TIMEOUT);
                    assertThat(received).isNotNull();
                    assertThat(new String(received.getData(), UTF_8)).isEqualTo(payload);
                    assertThat(received.hasHeaders()).isFalse();
                    assertThat(output).contains("Skipping Spring header 'invalid-value'");
                    assertThat(output).doesNotContain("bad\r\nvalue");
                }
            });
        } finally {
            mapperLogger.setLevel(originalLevel);
        }
    }

    @Test
    void messageProducerPreservesNatsHeadersAsSpringHeadersForIssue65() throws Exception {
        try (NatsBinderTestServer ts = new NatsBinderTestServer()) {
            this.contextRunner.withPropertyValues("nats.spring.server=" + ts.getURI()).run(context -> {
                Connection conn = context.getBean(Connection.class);
                assertConnected(conn, ts.getURI());

                try (BinderFixture fixture = newGlobalBinder(ts.getURI())) {
                    String subject = "producer.headers.issue65";
                    String payload = "headers survive dispatch";
                    ConsumerDestination from = fixture.provisioner().provisionConsumerDestination(subject, "", null);
                    NatsMessageProducer producer =
                            (NatsMessageProducer) fixture.binder().createConsumerEndpoint(from, "", null);
                    CompletableFuture<org.springframework.messaging.Message<?>> received = new CompletableFuture<>();
                    DirectChannel output = new DirectChannel();
                    output.subscribe(received::complete);
                    producer.setOutputChannel(output);

                    try {
                        producer.start();
                        fixture.connection().flush(FLUSH_TIMEOUT);

                        Headers headers = headersWithTrace("dispatch-123")
                                .put("custom-list", List.of("first", "second"))
                                .put("Subject", "spoofed.subject")
                                .put("replyChannel", "spoofed.reply");
                        conn.publish(subject, "actual.reply", headers, payload.getBytes(UTF_8));
                        conn.flush(FLUSH_TIMEOUT);

                        org.springframework.messaging.Message<?> message = received.get(5, TimeUnit.SECONDS);
                        assertThat(payloadText(message.getPayload())).isEqualTo(payload);
                        assertThat(message.getHeaders()).containsEntry(TRACE_HEADER, "dispatch-123");
                        assertThat(message.getHeaders()).containsEntry("custom-list", List.of("first", "second"));
                        assertThat(message.getHeaders()).containsEntry(NatsMessageProducer.SUBJECT, subject);
                        assertThat(message.getHeaders()).containsEntry(MessageHeaders.REPLY_CHANNEL, "actual.reply");
                    } finally {
                        producer.stop();
                    }
                }
            });
        }
    }

    @Test
    void customHeadersRoundTripThroughBindProducerAndBindConsumerForIssue65() throws Exception {
        try (NatsBinderTestServer ts = new NatsBinderTestServer()) {
            this.contextRunner.withPropertyValues("nats.spring.server=" + ts.getURI()).run(context -> {
                Connection conn = context.getBean(Connection.class);
                assertConnected(conn, ts.getURI());

                try (BinderFixture fixture = newGlobalBinder(ts.getURI())) {
                    fixture.binder().setApplicationContext(context.getSourceApplicationContext(GenericApplicationContext.class));
                    String subject = "binder.headers.issue65";
                    String payload = "headers survive round trip";
                    DirectChannel output = new DirectChannel();
                    DirectChannel input = new DirectChannel();
                    CompletableFuture<org.springframework.messaging.Message<?>> received = new CompletableFuture<>();
                    input.subscribe(received::complete);
                    Binding<MessageChannel> consumerBinding = null;
                    Binding<MessageChannel> producerBinding = null;

                    try {
                        consumerBinding = fixture.binder().bindConsumer(subject, "", input,
                                new ExtendedConsumerProperties<>(new NatsConsumerProperties()));
                        producerBinding = fixture.binder().bindProducer(subject, output,
                                new ExtendedProducerProperties<>(new NatsProducerProperties()));
                        fixture.connection().flush(FLUSH_TIMEOUT);

                        output.send(MessageBuilder.withPayload(payload)
                                .setHeader(TRACE_HEADER, "roundtrip-123")
                                .build());
                        fixture.connection().flush(FLUSH_TIMEOUT);

                        org.springframework.messaging.Message<?> message = received.get(5, TimeUnit.SECONDS);
                        assertThat(payloadText(message.getPayload())).isEqualTo(payload);
                        assertThat(message.getHeaders()).containsEntry(TRACE_HEADER, "roundtrip-123");
                    } finally {
                        if (producerBinding != null) {
                            producerBinding.unbind();
                        }
                        if (consumerBinding != null) {
                            consumerBinding.unbind();
                        }
                    }
                }
            });
        }
    }

    @Test
    void explicitHeaderModeHeadersRoundTripThroughProducerAndConsumerBindings() throws Exception {
        try (NatsBinderTestServer ts = new NatsBinderTestServer()) {
            this.contextRunner.withPropertyValues("nats.spring.server=" + ts.getURI()).run(context -> {
                Connection conn = context.getBean(Connection.class);
                assertConnected(conn, ts.getURI());

                try (BinderFixture fixture = newGlobalBinder(ts.getURI())) {
                    fixture.binder().setApplicationContext(context.getSourceApplicationContext(GenericApplicationContext.class));
                    String subject = "binder.headers.explicit";
                    String payload = "explicit native headers";
                    DirectChannel output = new DirectChannel();
                    DirectChannel input = new DirectChannel();
                    CompletableFuture<org.springframework.messaging.Message<?>> received = new CompletableFuture<>();
                    input.subscribe(received::complete);
                    ExtendedProducerProperties<NatsProducerProperties> producerProperties =
                            new ExtendedProducerProperties<>(new NatsProducerProperties());
                    producerProperties.setHeaderMode(HeaderMode.headers);
                    ExtendedConsumerProperties<NatsConsumerProperties> consumerProperties =
                            new ExtendedConsumerProperties<>(new NatsConsumerProperties());
                    consumerProperties.setHeaderMode(HeaderMode.headers);
                    Binding<MessageChannel> consumerBinding = null;
                    Binding<MessageChannel> producerBinding = null;

                    try {
                        consumerBinding = fixture.binder().bindConsumer(subject, "", input, consumerProperties);
                        producerBinding = fixture.binder().bindProducer(subject, output, producerProperties);
                        fixture.connection().flush(FLUSH_TIMEOUT);

                        output.send(MessageBuilder.withPayload(payload)
                                .setHeader(TRACE_HEADER, "explicit")
                                .build());

                        org.springframework.messaging.Message<?> message = received.get(5, TimeUnit.SECONDS);
                        assertThat(payloadText(message.getPayload())).isEqualTo(payload);
                        assertThat(message.getHeaders()).containsEntry(TRACE_HEADER, "explicit");
                    } finally {
                        if (producerBinding != null) {
                            producerBinding.unbind();
                        }
                        if (consumerBinding != null) {
                            consumerBinding.unbind();
                        }
                    }
                }
            });
        }
    }

    @Test
    void jetStreamProducerPublishesPersistedMessageForIssue52() throws Exception {
        try (NatsBinderTestServer ts = new NatsBinderTestServer(new String[]{"-js"}, false)) {
            this.contextRunner.withPropertyValues("nats.spring.server=" + ts.getURI()).run(context -> {
                Connection conn = context.getBean(Connection.class);
                assertConnected(conn, ts.getURI());

                try (BinderFixture fixture = newGlobalBinder(ts.getURI())) {
                    fixture.binder().setApplicationContext(context.getSourceApplicationContext(GenericApplicationContext.class));
                    String stream = uniqueNatsName("JS_PRODUCER");
                    String durable = uniqueNatsName("js_reader");
                    String subject = uniqueSubject("jetstream.producer.issue52");
                    String payload = "persist me";
                    addMemoryStream(conn, stream, subject);

                    DirectChannel output = new DirectChannel();
                    ExtendedProducerProperties<NatsProducerProperties> producerProperties =
                            new ExtendedProducerProperties<>(new NatsProducerProperties());
                    producerProperties.getExtension().setJetStream(true);
                    producerProperties.getExtension().setStreamName(stream);
                    Binding<MessageChannel> producerBinding = null;

                    try {
                        producerBinding = fixture.binder().bindProducer(subject, output, producerProperties);

                        output.send(MessageBuilder.withPayload(payload)
                                .setHeader(TRACE_HEADER, "js-producer")
                                .build());

                        JetStream js = conn.jetStream();
                        JetStreamSubscription sub = js.subscribe(subject, PullSubscribeOptions.builder()
                                .stream(stream)
                                .durable(durable)
                                .build());
                        List<Message> messages = sub.fetch(1, FLUSH_TIMEOUT);

                        assertThat(messages).hasSize(1);
                        Message received = messages.get(0);
                        assertThat(received.isJetStream()).isTrue();
                        assertThat(new String(received.getData(), UTF_8)).isEqualTo(payload);
                        assertThat(received.getHeaders().getFirst(TRACE_HEADER)).isEqualTo("js-producer");
                        received.ack();
                    } finally {
                        if (producerBinding != null) {
                            producerBinding.unbind();
                        }
                    }
                }
            });
        }
    }

    @Test
    void jetStreamProducerCanResolveStreamFromSubjectForIssue52() throws Exception {
        try (NatsBinderTestServer ts = new NatsBinderTestServer(new String[]{"-js"}, false)) {
            this.contextRunner.withPropertyValues("nats.spring.server=" + ts.getURI()).run(context -> {
                Connection conn = context.getBean(Connection.class);
                assertConnected(conn, ts.getURI());

                try (BinderFixture fixture = newGlobalBinder(ts.getURI())) {
                    fixture.binder().setApplicationContext(context.getSourceApplicationContext(GenericApplicationContext.class));
                    String stream = uniqueNatsName("JS_RESOLVE");
                    String durable = uniqueNatsName("js_resolve");
                    String subject = uniqueSubject("jetstream.resolve.issue52");
                    String payload = "resolve stream";
                    addMemoryStream(conn, stream, subject);

                    DirectChannel output = new DirectChannel();
                    ExtendedProducerProperties<NatsProducerProperties> producerProperties =
                            new ExtendedProducerProperties<>(new NatsProducerProperties());
                    producerProperties.getExtension().setJetStream(true);
                    Binding<MessageChannel> producerBinding = null;

                    try {
                        producerBinding = fixture.binder().bindProducer(subject, output, producerProperties);

                        output.send(MessageBuilder.withPayload(payload)
                                .setHeader(TRACE_HEADER, "js-resolve")
                                .build());

                        JetStreamSubscription sub = conn.jetStream().subscribe(subject, PullSubscribeOptions.builder()
                                .stream(stream)
                                .durable(durable)
                                .build());
                        Message received = sub.fetch(1, FLUSH_TIMEOUT).get(0);
                        assertThat(new String(received.getData(), UTF_8)).isEqualTo(payload);
                        assertThat(received.getHeaders().getFirst(TRACE_HEADER)).isEqualTo("js-resolve");
                        received.ack();
                    } finally {
                        if (producerBinding != null) {
                            producerBinding.unbind();
                        }
                    }
                }
            });
        }
    }

    @Test
    void jetStreamProducerCanResolveStreamFromSubjectWithoutHeadersForIssue52() throws Exception {
        try (NatsBinderTestServer ts = new NatsBinderTestServer(new String[]{"-js"}, false)) {
            this.contextRunner.withPropertyValues("nats.spring.server=" + ts.getURI()).run(context -> {
                Connection conn = context.getBean(Connection.class);
                assertConnected(conn, ts.getURI());

                try (BinderFixture fixture = newGlobalBinder(ts.getURI())) {
                    fixture.binder().setApplicationContext(context.getSourceApplicationContext(GenericApplicationContext.class));
                    String stream = uniqueNatsName("JS_RESOLVE_NO_HEADERS");
                    String durable = uniqueNatsName("js_resolve_no_headers");
                    String subject = uniqueSubject("jetstream.resolve.no.headers.issue52");
                    String payload = "resolve stream without headers";
                    addMemoryStream(conn, stream, subject);

                    DirectChannel output = new DirectChannel();
                    ExtendedProducerProperties<NatsProducerProperties> producerProperties =
                            new ExtendedProducerProperties<>(new NatsProducerProperties());
                    producerProperties.setHeaderMode(HeaderMode.none);
                    producerProperties.getExtension().setJetStream(true);
                    Binding<MessageChannel> producerBinding = null;

                    try {
                        producerBinding = fixture.binder().bindProducer(subject, output, producerProperties);

                        output.send(MessageBuilder.withPayload(payload)
                                .setHeader(TRACE_HEADER, "suppressed")
                                .build());

                        JetStreamSubscription sub = conn.jetStream().subscribe(subject, PullSubscribeOptions.builder()
                                .stream(stream)
                                .durable(durable)
                                .build());
                        List<Message> messages = sub.fetch(1, FLUSH_TIMEOUT);
                        assertThat(messages).hasSize(1);
                        Message received = messages.get(0);
                        assertThat(new String(received.getData(), UTF_8)).isEqualTo(payload);
                        assertThat(received.hasHeaders()).isFalse();
                        received.ack();
                    } finally {
                        if (producerBinding != null) {
                            producerBinding.unbind();
                        }
                    }
                }
            });
        }
    }

    @Test
    void jetStreamProducerHonorsHeaderModeNoneForIssue52() throws Exception {
        try (NatsBinderTestServer ts = new NatsBinderTestServer(new String[]{"-js"}, false)) {
            this.contextRunner.withPropertyValues("nats.spring.server=" + ts.getURI()).run(context -> {
                Connection conn = context.getBean(Connection.class);
                assertConnected(conn, ts.getURI());

                try (BinderFixture fixture = newGlobalBinder(ts.getURI())) {
                    fixture.binder().setApplicationContext(context.getSourceApplicationContext(GenericApplicationContext.class));
                    String stream = uniqueNatsName("JS_NO_HEADERS");
                    String durable = uniqueNatsName("js_no_headers");
                    String subject = uniqueSubject("jetstream.no.headers.issue52");
                    String payload = "no native headers";
                    addMemoryStream(conn, stream, subject);

                    DirectChannel output = new DirectChannel();
                    ExtendedProducerProperties<NatsProducerProperties> producerProperties =
                            new ExtendedProducerProperties<>(new NatsProducerProperties());
                    producerProperties.setHeaderMode(HeaderMode.none);
                    producerProperties.getExtension().setJetStream(true);
                    producerProperties.getExtension().setStreamName(stream);
                    Binding<MessageChannel> producerBinding = null;

                    try {
                        producerBinding = fixture.binder().bindProducer(subject, output, producerProperties);

                        output.send(MessageBuilder.withPayload(payload)
                                .setHeader(TRACE_HEADER, "suppressed")
                                .build());

                        JetStreamSubscription sub = conn.jetStream().subscribe(subject, PullSubscribeOptions.builder()
                                .stream(stream)
                                .durable(durable)
                                .build());
                        Message received = sub.fetch(1, FLUSH_TIMEOUT).get(0);
                        assertThat(new String(received.getData(), UTF_8)).isEqualTo(payload);
                        assertThat(received.hasHeaders()).isFalse();
                        received.ack();
                    } finally {
                        if (producerBinding != null) {
                            producerBinding.unbind();
                        }
                    }
                }
            });
        }
    }

    @Test
    void jetStreamProducerProvisioningRejectsExistingStreamWithoutDestinationSubjectForIssue52() throws Exception {
        try (NatsBinderTestServer ts = new NatsBinderTestServer(new String[]{"-js"}, false)) {
            this.contextRunner.withPropertyValues("nats.spring.server=" + ts.getURI()).run(context -> {
                Connection conn = context.getBean(Connection.class);
                assertConnected(conn, ts.getURI());

                try (BinderFixture fixture = newGlobalBinder(ts.getURI())) {
                    fixture.binder().setApplicationContext(context.getSourceApplicationContext(GenericApplicationContext.class));
                    String stream = uniqueNatsName("JS_PROVISION_PRODUCER");
                    String existingSubject = uniqueSubject("jetstream.provision.existing.issue52");
                    String subject = uniqueSubject("jetstream.provision.producer.issue52");
                    addMemoryStream(conn, stream, existingSubject);

                    DirectChannel output = new DirectChannel();
                    ExtendedProducerProperties<NatsProducerProperties> producerProperties =
                            new ExtendedProducerProperties<>(new NatsProducerProperties());
                    producerProperties.getExtension().setJetStream(true);
                    producerProperties.getExtension().setStreamName(stream);
                    producerProperties.getExtension().setProvisionStream(true);

                    assertThatThrownBy(() -> fixture.binder().bindProducer(subject, output, producerProperties))
                            .isInstanceOf(BinderException.class)
                            .satisfies(exp -> assertThat(exp.getCause())
                                    .isInstanceOf(IllegalStateException.class)
                                    .hasMessageContaining("does not include subject " + subject));
                    assertThat(streamInfo(conn, stream).getConfiguration().getSubjects())
                            .containsExactly(existingSubject);
                }
            });
        }
    }

    @Test
    void jetStreamProducerProvisioningRequiresStreamNameForIssue52() throws Exception {
        try (NatsBinderTestServer ts = new NatsBinderTestServer(new String[]{"-js"}, false)) {
            this.contextRunner.withPropertyValues("nats.spring.server=" + ts.getURI()).run(context -> {
                Connection conn = context.getBean(Connection.class);
                assertConnected(conn, ts.getURI());

                try (BinderFixture fixture = newGlobalBinder(ts.getURI())) {
                    fixture.binder().setApplicationContext(context.getSourceApplicationContext(GenericApplicationContext.class));
                    String subject = uniqueSubject("jetstream.provision.missing.stream.issue52");
                    DirectChannel output = new DirectChannel();
                    ExtendedProducerProperties<NatsProducerProperties> producerProperties =
                            new ExtendedProducerProperties<>(new NatsProducerProperties());
                    producerProperties.getExtension().setJetStream(true);
                    producerProperties.getExtension().setProvisionStream(true);

                    assertThatThrownBy(() -> fixture.binder().bindProducer(subject, output, producerProperties))
                            .isInstanceOf(BinderException.class)
                            .satisfies(exp -> assertThat(exp.getCause())
                                    .isInstanceOf(IllegalStateException.class)
                                    .hasMessageContaining("stream-name"));
                }
            });
        }
    }

    @Test
    void jetStreamProducerProvisioningAppliesStreamOptionsForIssue52() throws Exception {
        try (NatsBinderTestServer ts = new NatsBinderTestServer(new String[]{"-js"}, false)) {
            this.contextRunner.withPropertyValues("nats.spring.server=" + ts.getURI()).run(context -> {
                Connection conn = context.getBean(Connection.class);
                assertConnected(conn, ts.getURI());

                try (BinderFixture fixture = newGlobalBinder(ts.getURI())) {
                    fixture.binder().setApplicationContext(context.getSourceApplicationContext(GenericApplicationContext.class));
                    String stream = uniqueNatsName("JS_PROVISION_OPTIONS");
                    String subject = uniqueSubject("jetstream.provision.options.issue52");
                    Binding<MessageChannel> producerBinding = null;

                    try {
                        producerBinding = bindProvisionedJetStreamProducer(fixture, subject, stream, StorageType.Memory, 1);

                        StreamConfiguration configuration = streamInfo(conn, stream).getConfiguration();
                        assertThat(configuration.getSubjects()).containsExactly(subject);
                        assertThat(configuration.getStorageType()).isEqualTo(StorageType.Memory);
                        assertThat(configuration.getReplicas()).isEqualTo(1);
                    } finally {
                        if (producerBinding != null) {
                            producerBinding.unbind();
                        }
                    }
                }
            });
        }
    }

    @Test
    void jetStreamProducerProvisioningReportsInvalidStreamConfigurationForIssue52() throws Exception {
        try (NatsBinderTestServer ts = new NatsBinderTestServer(new String[]{"-js"}, false)) {
            this.contextRunner.withPropertyValues("nats.spring.server=" + ts.getURI()).run(context -> {
                Connection conn = context.getBean(Connection.class);
                assertConnected(conn, ts.getURI());

                try (BinderFixture fixture = newGlobalBinder(ts.getURI())) {
                    fixture.binder().setApplicationContext(context.getSourceApplicationContext(GenericApplicationContext.class));
                    String stream = uniqueNatsName("JS_PROVISION_INVALID");
                    String subject = uniqueSubject("jetstream.provision.invalid.issue52");
                    DirectChannel output = new DirectChannel();
                    ExtendedProducerProperties<NatsProducerProperties> producerProperties =
                            new ExtendedProducerProperties<>(new NatsProducerProperties());
                    producerProperties.getExtension().setJetStream(true);
                    producerProperties.getExtension().setStreamName(stream);
                    producerProperties.getExtension().setProvisionStream(true);
                    producerProperties.getExtension().setStreamReplicas(0);

                    assertThatThrownBy(() -> fixture.binder().bindProducer(subject, output, producerProperties))
                            .isInstanceOf(BinderException.class)
                            .satisfies(exp -> assertThat(exp.getCause())
                                    .isInstanceOf(IllegalStateException.class)
                                    .hasMessageContaining("Failed to provision NATS JetStream stream " + stream));
                }
            });
        }
    }

    @Test
    void jetStreamProducerProvisioningKeepsExistingWildcardSubjectForIssue52() throws Exception {
        try (NatsBinderTestServer ts = new NatsBinderTestServer(new String[]{"-js"}, false)) {
            this.contextRunner.withPropertyValues("nats.spring.server=" + ts.getURI()).run(context -> {
                Connection conn = context.getBean(Connection.class);
                assertConnected(conn, ts.getURI());

                try (BinderFixture fixture = newGlobalBinder(ts.getURI())) {
                    fixture.binder().setApplicationContext(context.getSourceApplicationContext(GenericApplicationContext.class));
                    String stream = uniqueNatsName("JS_PROVISION_WILDCARD");
                    String base = uniqueSubject("jetstream.provision.wildcard.issue52");
                    String wildcardSubject = base + ".>";
                    String subject = base + ".created";
                    String durable = uniqueNatsName("js_provision_wildcard");
                    String payload = "matched by wildcard";
                    addMemoryStream(conn, stream, wildcardSubject);

                    DirectChannel output = new DirectChannel();
                    ExtendedProducerProperties<NatsProducerProperties> producerProperties =
                            new ExtendedProducerProperties<>(new NatsProducerProperties());
                    producerProperties.getExtension().setJetStream(true);
                    producerProperties.getExtension().setStreamName(stream);
                    producerProperties.getExtension().setProvisionStream(true);
                    Binding<MessageChannel> producerBinding = null;

                    try {
                        producerBinding = fixture.binder().bindProducer(subject, output, producerProperties);

                        output.send(MessageBuilder.withPayload(payload).build());

                        assertThat(streamInfo(conn, stream).getConfiguration().getSubjects())
                                .containsExactly(wildcardSubject);

                        JetStreamSubscription sub = conn.jetStream().subscribe(subject, PullSubscribeOptions.builder()
                                .stream(stream)
                                .durable(durable)
                                .build());
                        Message received = sub.fetch(1, FLUSH_TIMEOUT).get(0);
                        assertThat(new String(received.getData(), UTF_8)).isEqualTo(payload);
                        received.ack();
                    } finally {
                        if (producerBinding != null) {
                            producerBinding.unbind();
                        }
                    }
                }
            });
        }
    }

    @Test
    void jetStreamProducerProvisioningHandlesExistingSubjectPatternsForIssue52() throws Exception {
        try (NatsBinderTestServer ts = new NatsBinderTestServer(new String[]{"-js"}, false)) {
            this.contextRunner.withPropertyValues("nats.spring.server=" + ts.getURI()).run(context -> {
                Connection conn = context.getBean(Connection.class);
                assertConnected(conn, ts.getURI());

                try (BinderFixture fixture = newGlobalBinder(ts.getURI())) {
                    fixture.binder().setApplicationContext(context.getSourceApplicationContext(GenericApplicationContext.class));
                    String exactStream = uniqueNatsName("JS_PROVISION_EXACT");
                    String exactSubject = uniqueSubject("jetstream.provision.exact.issue52");
                    String shortStream = uniqueNatsName("JS_PROVISION_SHORT");
                    String shortSubject = uniqueSubject("jetstream.provision.short.issue52");
                    String shortPattern = shortSubject + ".>";
                    String longStream = uniqueNatsName("JS_PROVISION_LONG");
                    String longSubject = uniqueSubject("jetstream.provision.long.issue52");
                    String longExistingSubject = longSubject + ".extra";
                    String prefixStream = uniqueNatsName("JS_PROVISION_PREFIX");
                    String prefixExistingSubject = uniqueSubject("jetstream.provision.prefix.issue52");
                    String prefixSubject = prefixExistingSubject + ".extra";
                    String defaultStream = uniqueNatsName("JS_PROVISION_DEFAULT");
                    String defaultSubject = uniqueSubject("jetstream.provision.default.issue52");
                    addMemoryStream(conn, exactStream, exactSubject);
                    addMemoryStream(conn, shortStream, shortPattern);
                    addMemoryStream(conn, longStream, longExistingSubject);
                    addMemoryStream(conn, prefixStream, prefixExistingSubject);

                    Binding<MessageChannel> exactBinding = null;
                    Binding<MessageChannel> defaultBinding = null;

                    try {
                        exactBinding = bindProvisionedJetStreamProducer(fixture, exactSubject, exactStream, null, null);
                        defaultBinding = bindProvisionedJetStreamProducer(fixture, defaultSubject, defaultStream, null, null);

                        assertThat(streamInfo(conn, exactStream).getConfiguration().getSubjects())
                                .containsExactly(exactSubject);
                        assertStreamProvisioningFailsForMissingSubject(fixture, shortSubject, shortStream);
                        assertStreamProvisioningFailsForMissingSubject(fixture, longSubject, longStream);
                        assertStreamProvisioningFailsForMissingSubject(fixture, prefixSubject, prefixStream);
                        assertThat(streamInfo(conn, shortStream).getConfiguration().getSubjects())
                                .containsExactly(shortPattern);
                        assertThat(streamInfo(conn, longStream).getConfiguration().getSubjects())
                                .containsExactly(longExistingSubject);
                        assertThat(streamInfo(conn, prefixStream).getConfiguration().getSubjects())
                                .containsExactly(prefixExistingSubject);
                        StreamConfiguration defaultConfiguration = streamInfo(conn, defaultStream).getConfiguration();
                        assertThat(defaultConfiguration.getSubjects()).containsExactly(defaultSubject);
                        assertThat(defaultConfiguration.getStorageType()).isEqualTo(StorageType.File);
                        assertThat(defaultConfiguration.getReplicas()).isEqualTo(1);
                    } finally {
                        if (defaultBinding != null) {
                            defaultBinding.unbind();
                        }
                        if (exactBinding != null) {
                            exactBinding.unbind();
                        }
                    }
                }
            });
        }
    }

    @Test
    void concurrentJetStreamProducerProvisioningDoesNotLoseSubjectsForIssue52() throws Exception {
        try (NatsBinderTestServer ts = new NatsBinderTestServer(new String[]{"-js"}, false)) {
            this.contextRunner.withPropertyValues("nats.spring.server=" + ts.getURI()).run(context -> {
                Connection conn = context.getBean(Connection.class);
                assertConnected(conn, ts.getURI());

                try (BinderFixture firstFixture = newGlobalBinder(ts.getURI());
                     BinderFixture secondFixture = newGlobalBinder(ts.getURI())) {
                    firstFixture.binder().setApplicationContext(context.getSourceApplicationContext(GenericApplicationContext.class));
                    secondFixture.binder().setApplicationContext(context.getSourceApplicationContext(GenericApplicationContext.class));
                    String stream = uniqueNatsName("JS_PROVISION_CONCURRENT");
                    String firstSubject = uniqueSubject("jetstream.provision.concurrent.first.issue52");
                    String secondSubject = uniqueSubject("jetstream.provision.concurrent.second.issue52");
                    CountDownLatch start = new CountDownLatch(1);
                    AtomicReference<Binding<MessageChannel>> firstBinding = new AtomicReference<>();
                    AtomicReference<Binding<MessageChannel>> secondBinding = new AtomicReference<>();
                    AtomicReference<Throwable> firstFailure = new AtomicReference<>();
                    AtomicReference<Throwable> secondFailure = new AtomicReference<>();

                    try {
                        CompletableFuture<Void> first = CompletableFuture.runAsync(() ->
                                bindProvisionedJetStreamProducerAfter(start, firstFixture, firstSubject, stream,
                                        firstBinding, firstFailure));
                        CompletableFuture<Void> second = CompletableFuture.runAsync(() ->
                                bindProvisionedJetStreamProducerAfter(start, secondFixture, secondSubject, stream,
                                        secondBinding, secondFailure));

                        start.countDown();
                        CompletableFuture.allOf(first, second).get(5, TimeUnit.SECONDS);

                        int successfulBindings = (firstBinding.get() == null ? 0 : 1)
                                + (secondBinding.get() == null ? 0 : 1);
                        int failedBindings = (firstFailure.get() == null ? 0 : 1)
                                + (secondFailure.get() == null ? 0 : 1);
                        assertThat(successfulBindings).isEqualTo(1);
                        assertThat(failedBindings).isEqualTo(1);

                        Throwable failure = firstFailure.get() == null ? secondFailure.get() : firstFailure.get();
                        assertThat(failure).isInstanceOf(BinderException.class)
                                .satisfies(exp -> assertThat(exp.getCause())
                                        .isInstanceOf(IllegalStateException.class)
                                        .hasMessageContaining("does not include subject"));
                        assertThat(streamInfo(conn, stream).getConfiguration().getSubjects())
                                .hasSize(1)
                                .containsAnyOf(firstSubject, secondSubject);
                    } finally {
                        if (firstBinding.get() != null) {
                            firstBinding.get().unbind();
                        }
                        if (secondBinding.get() != null) {
                            secondBinding.get().unbind();
                        }
                    }
                }
            });
        }
    }

    @Test
    void jetStreamConsumerReceivesAndAcknowledgesMessageForIssue52() throws Exception {
        try (NatsBinderTestServer ts = new NatsBinderTestServer(new String[]{"-js"}, false)) {
            this.contextRunner.withPropertyValues("nats.spring.server=" + ts.getURI()).run(context -> {
                Connection conn = context.getBean(Connection.class);
                assertConnected(conn, ts.getURI());

                try (BinderFixture fixture = newGlobalBinder(ts.getURI())) {
                    fixture.binder().setApplicationContext(context.getSourceApplicationContext(GenericApplicationContext.class));
                    String stream = uniqueNatsName("JS_CONSUMER");
                    String durable = uniqueNatsName("js_consumer");
                    String subject = uniqueSubject("jetstream.consumer.issue52");
                    String payload = "deliver then ack";
                    addMemoryStream(conn, stream, subject);
                    addConsumer(conn, stream, durable, subject);

                    JetStream js = conn.jetStream();
                    js.publish(subject, headersWithTrace("js-consumer"), payload.getBytes(UTF_8));

                    DirectChannel input = new DirectChannel();
                    CompletableFuture<org.springframework.messaging.Message<?>> received = new CompletableFuture<>();
                    input.subscribe(received::complete);
                    ExtendedConsumerProperties<NatsConsumerProperties> consumerProperties =
                            new ExtendedConsumerProperties<>(new NatsConsumerProperties());
                    consumerProperties.getExtension().setJetStream(true);
                    consumerProperties.getExtension().setStreamName(stream);
                    consumerProperties.getExtension().setConsumerName(durable);
                    Binding<MessageChannel> consumerBinding = null;

                    try {
                        consumerBinding = fixture.binder().bindConsumer(subject, "", input, consumerProperties);
                        fixture.connection().flush(FLUSH_TIMEOUT);

                        org.springframework.messaging.Message<?> message = received.get(5, TimeUnit.SECONDS);
                        assertThat(payloadText(message.getPayload())).isEqualTo(payload);
                        assertThat(message.getHeaders()).containsEntry(TRACE_HEADER, "js-consumer");
                        assertThat(message.getHeaders()).containsEntry(NatsMessageProducer.SUBJECT, subject);

                        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
                                assertThat(consumerInfo(conn, stream, durable).getNumAckPending()).isZero());
                    } finally {
                        if (consumerBinding != null) {
                            consumerBinding.unbind();
                        }
                    }
                }
            });
        }
    }

    @Test
    void jetStreamConsumerWithGroupReceivesMessageForIssue52() throws Exception {
        try (NatsBinderTestServer ts = new NatsBinderTestServer(new String[]{"-js"}, false)) {
            this.contextRunner.withPropertyValues("nats.spring.server=" + ts.getURI()).run(context -> {
                Connection conn = context.getBean(Connection.class);
                assertConnected(conn, ts.getURI());

                try (BinderFixture fixture = newGlobalBinder(ts.getURI())) {
                    fixture.binder().setApplicationContext(context.getSourceApplicationContext(GenericApplicationContext.class));
                    String stream = uniqueNatsName("JS_GROUP");
                    String group = uniqueNatsName("js_group");
                    String subject = uniqueSubject("jetstream.group.issue52");
                    String payload = "deliver to group";
                    addMemoryStream(conn, stream, subject);
                    addConsumer(conn, stream, group, subject);

                    DirectChannel input = new DirectChannel();
                    CompletableFuture<org.springframework.messaging.Message<?>> received = new CompletableFuture<>();
                    input.subscribe(received::complete);
                    ExtendedConsumerProperties<NatsConsumerProperties> consumerProperties =
                            new ExtendedConsumerProperties<>(new NatsConsumerProperties());
                    consumerProperties.getExtension().setJetStream(true);
                    consumerProperties.getExtension().setStreamName(stream);
                    Binding<MessageChannel> consumerBinding = null;

                    try {
                        consumerBinding = fixture.binder().bindConsumer(subject, group, input, consumerProperties);
                        fixture.connection().flush(FLUSH_TIMEOUT);

                        conn.jetStream().publish(subject, payload.getBytes(UTF_8));

                        org.springframework.messaging.Message<?> message = received.get(5, TimeUnit.SECONDS);
                        assertThat(payloadText(message.getPayload())).isEqualTo(payload);
                        assertThat(message.getHeaders()).containsEntry(NatsMessageProducer.SUBJECT, subject);
                    } finally {
                        if (consumerBinding != null) {
                            consumerBinding.unbind();
                        }
                    }
                }
            });
        }
    }

    @Test
    void jetStreamConsumerRequiresStreamAndConsumerIdentityForIssue52() throws Exception {
        try (NatsBinderTestServer ts = new NatsBinderTestServer(new String[]{"-js"}, false)) {
            this.contextRunner.withPropertyValues("nats.spring.server=" + ts.getURI()).run(context -> {
                Connection conn = context.getBean(Connection.class);
                assertConnected(conn, ts.getURI());

                try (BinderFixture fixture = newGlobalBinder(ts.getURI())) {
                    fixture.binder().setApplicationContext(context.getSourceApplicationContext(GenericApplicationContext.class));
                    String stream = uniqueNatsName("JS_CONSUMER_IDENTITY");
                    String consumer = uniqueNatsName("js_consumer_identity");
                    String subject = uniqueSubject("jetstream.consumer.identity.issue52");
                    DirectChannel input = new DirectChannel();
                    ExtendedConsumerProperties<NatsConsumerProperties> missingStreamProperties =
                            new ExtendedConsumerProperties<>(new NatsConsumerProperties());
                    missingStreamProperties.getExtension().setJetStream(true);
                    missingStreamProperties.getExtension().setConsumerName(consumer);

                    assertThatThrownBy(() -> fixture.binder().bindConsumer(subject, "", input, missingStreamProperties))
                            .isInstanceOf(BinderException.class)
                            .satisfies(exp -> assertThat(exp.getCause())
                                    .isInstanceOf(IllegalStateException.class)
                                    .hasMessage("NATS JetStream consumers require stream-name"));

                    addMemoryStream(conn, stream, subject);
                    ExtendedConsumerProperties<NatsConsumerProperties> missingConsumerProperties =
                            new ExtendedConsumerProperties<>(new NatsConsumerProperties());
                    missingConsumerProperties.getExtension().setJetStream(true);
                    missingConsumerProperties.getExtension().setStreamName(stream);

                    assertThatThrownBy(() -> fixture.binder().bindConsumer(subject, "", input, missingConsumerProperties))
                            .isInstanceOf(BinderException.class)
                            .satisfies(exp -> assertThat(exp.getCause())
                                    .isInstanceOf(IllegalStateException.class)
                                    .hasMessage("NATS JetStream consumers require consumer-name or a consumer group"));
                }
            });
        }
    }

    @Test
    void jetStreamConsumerRequiresExistingNamedConsumerForIssue52() throws Exception {
        try (NatsBinderTestServer ts = new NatsBinderTestServer(new String[]{"-js"}, false)) {
            this.contextRunner.withPropertyValues("nats.spring.server=" + ts.getURI()).run(context -> {
                Connection conn = context.getBean(Connection.class);
                assertConnected(conn, ts.getURI());

                try (BinderFixture fixture = newGlobalBinder(ts.getURI())) {
                    fixture.binder().setApplicationContext(context.getSourceApplicationContext(GenericApplicationContext.class));
                    String stream = uniqueNatsName("JS_MISSING_CONSUMER");
                    String consumer = uniqueNatsName("js_missing_consumer");
                    String subject = uniqueSubject("jetstream.missing.consumer.issue52");
                    addMemoryStream(conn, stream, subject);
                    DirectChannel input = new DirectChannel();
                    ExtendedConsumerProperties<NatsConsumerProperties> consumerProperties =
                            new ExtendedConsumerProperties<>(new NatsConsumerProperties());
                    consumerProperties.getExtension().setJetStream(true);
                    consumerProperties.getExtension().setStreamName(stream);
                    consumerProperties.getExtension().setConsumerName(consumer);

                    assertThatThrownBy(() -> fixture.binder().bindConsumer(subject, "", input, consumerProperties))
                            .isInstanceOf(BinderException.class)
                            .satisfies(exp -> assertThat(exp.getCause())
                                    .isInstanceOf(IllegalStateException.class)
                                    .hasMessageContaining("Failed to subscribe to NATS JetStream subject " + subject));
                }
            });
        }
    }

    @Test
    void jetStreamConsumerBindsExistingConsumerConfigurationForIssue52() throws Exception {
        try (NatsBinderTestServer ts = new NatsBinderTestServer(new String[]{"-js"}, false)) {
            this.contextRunner.withPropertyValues("nats.spring.server=" + ts.getURI()).run(context -> {
                Connection conn = context.getBean(Connection.class);
                assertConnected(conn, ts.getURI());

                try (BinderFixture fixture = newGlobalBinder(ts.getURI())) {
                    fixture.binder().setApplicationContext(context.getSourceApplicationContext(GenericApplicationContext.class));
                    String stream = uniqueNatsName("JS_EXISTING_CONSUMER");
                    String consumer = uniqueNatsName("js_existing_consumer");
                    String subject = uniqueSubject("jetstream.existing.consumer.issue52");
                    String payload = "configured externally";
                    addMemoryStream(conn, stream, subject);
                    addConsumer(conn, stream, consumer, subject, ConsumerConfiguration.builder()
                            .ackWait(Duration.ofMillis(500))
                            .maxDeliver(3));

                    DirectChannel input = new DirectChannel();
                    CompletableFuture<org.springframework.messaging.Message<?>> received = new CompletableFuture<>();
                    input.subscribe(received::complete);
                    ExtendedConsumerProperties<NatsConsumerProperties> consumerProperties =
                            new ExtendedConsumerProperties<>(new NatsConsumerProperties());
                    consumerProperties.getExtension().setJetStream(true);
                    consumerProperties.getExtension().setStreamName(stream);
                    consumerProperties.getExtension().setConsumerName(consumer);
                    Binding<MessageChannel> consumerBinding = null;

                    try {
                        consumerBinding = fixture.binder().bindConsumer(subject, "", input, consumerProperties);
                        fixture.connection().flush(FLUSH_TIMEOUT);

                        conn.jetStream().publish(subject, payload.getBytes(UTF_8));

                        org.springframework.messaging.Message<?> message = received.get(5, TimeUnit.SECONDS);
                        assertThat(payloadText(message.getPayload())).isEqualTo(payload);
                        ConsumerConfiguration configuration =
                                consumerInfo(conn, stream, consumer).getConsumerConfiguration();
                        assertThat(configuration.getAckWait()).isEqualTo(Duration.ofMillis(500));
                        assertThat(configuration.getMaxDeliver()).isEqualTo(3);
                        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
                                assertThat(consumerInfo(conn, stream, consumer).getNumAckPending()).isZero());
                    } finally {
                        if (consumerBinding != null) {
                            consumerBinding.unbind();
                        }
                    }
                }
            });
        }
    }

    @Test
    void jetStreamConsumerNacksWhenOutputChannelRejectsMessageForIssue52() throws Exception {
        try (NatsBinderTestServer ts = new NatsBinderTestServer(new String[]{"-js"}, false)) {
            this.contextRunner.withPropertyValues("nats.spring.server=" + ts.getURI()).run(context -> {
                Connection conn = context.getBean(Connection.class);
                assertConnected(conn, ts.getURI());

                try (BinderFixture fixture = newGlobalBinder(ts.getURI())) {
                    fixture.binder().setApplicationContext(context.getSourceApplicationContext(GenericApplicationContext.class));
                    String stream = uniqueNatsName("JS_REJECTED_SEND");
                    String durable = uniqueNatsName("js_rejected_send");
                    String subject = uniqueSubject("jetstream.rejected.send.issue52");
                    String payload = "redeliver rejected send";
                    addMemoryStream(conn, stream, subject);
                    addConsumer(conn, stream, durable, subject);

                    AtomicInteger attempts = new AtomicInteger();
                    CompletableFuture<org.springframework.messaging.Message<?>> redelivered = new CompletableFuture<>();
                    MessageChannel input = new MessageChannel() {
                        @Override
                        public boolean send(org.springframework.messaging.Message<?> message) {
                            if (attempts.incrementAndGet() == 1) {
                                return false;
                            }
                            redelivered.complete(message);
                            return true;
                        }

                        @Override
                        public boolean send(org.springframework.messaging.Message<?> message, long timeout) {
                            return send(message);
                        }
                    };
                    ExtendedConsumerProperties<NatsConsumerProperties> consumerProperties =
                            new ExtendedConsumerProperties<>(new NatsConsumerProperties());
                    consumerProperties.getExtension().setJetStream(true);
                    consumerProperties.getExtension().setStreamName(stream);
                    consumerProperties.getExtension().setConsumerName(durable);
                    Binding<MessageChannel> consumerBinding = null;

                    try {
                        consumerBinding = fixture.binder().bindConsumer(subject, "", input, consumerProperties);
                        fixture.connection().flush(FLUSH_TIMEOUT);

                        conn.jetStream().publish(subject, payload.getBytes(UTF_8));

                        org.springframework.messaging.Message<?> message = redelivered.get(5, TimeUnit.SECONDS);
                        assertThat(payloadText(message.getPayload())).isEqualTo(payload);
                        assertThat(attempts).hasValue(2);
                        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
                                assertThat(consumerInfo(conn, stream, durable).getNumAckPending()).isZero());
                    } finally {
                        if (consumerBinding != null) {
                            consumerBinding.unbind();
                        }
                    }
                }
            });
        }
    }

    @Test
    void jetStreamConsumerNacksWhenOutputChannelThrowsForIssue52() throws Exception {
        try (NatsBinderTestServer ts = new NatsBinderTestServer(new String[]{"-js"}, false)) {
            this.contextRunner.withPropertyValues("nats.spring.server=" + ts.getURI()).run(context -> {
                Connection conn = context.getBean(Connection.class);
                assertConnected(conn, ts.getURI());

                try (BinderFixture fixture = newGlobalBinder(ts.getURI())) {
                    fixture.binder().setApplicationContext(context.getSourceApplicationContext(GenericApplicationContext.class));
                    String stream = uniqueNatsName("JS_THROWING_SEND");
                    String durable = uniqueNatsName("js_throwing_send");
                    String subject = uniqueSubject("jetstream.throwing.send.issue52");
                    String payload = "redeliver thrown send";
                    addMemoryStream(conn, stream, subject);
                    addConsumer(conn, stream, durable, subject);

                    AtomicInteger attempts = new AtomicInteger();
                    CompletableFuture<org.springframework.messaging.Message<?>> redelivered = new CompletableFuture<>();
                    MessageChannel input = new MessageChannel() {
                        @Override
                        public boolean send(org.springframework.messaging.Message<?> message) {
                            if (attempts.incrementAndGet() == 1) {
                                throw new MessageHandlingException(message, "fail once");
                            }
                            redelivered.complete(message);
                            return true;
                        }

                        @Override
                        public boolean send(org.springframework.messaging.Message<?> message, long timeout) {
                            return send(message);
                        }
                    };
                    ExtendedConsumerProperties<NatsConsumerProperties> consumerProperties =
                            new ExtendedConsumerProperties<>(new NatsConsumerProperties());
                    consumerProperties.getExtension().setJetStream(true);
                    consumerProperties.getExtension().setStreamName(stream);
                    consumerProperties.getExtension().setConsumerName(durable);
                    Binding<MessageChannel> consumerBinding = null;

                    try {
                        consumerBinding = fixture.binder().bindConsumer(subject, "", input, consumerProperties);
                        fixture.connection().flush(FLUSH_TIMEOUT);

                        conn.jetStream().publish(subject, payload.getBytes(UTF_8));

                        org.springframework.messaging.Message<?> message = redelivered.get(5, TimeUnit.SECONDS);
                        assertThat(payloadText(message.getPayload())).isEqualTo(payload);
                        assertThat(attempts).hasValue(2);
                        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
                                assertThat(consumerInfo(conn, stream, durable).getNumAckPending()).isZero());
                    } finally {
                        if (consumerBinding != null) {
                            consumerBinding.unbind();
                        }
                    }
                }
            });
        }
    }

    @Test
    void jetStreamPolledConsumerReceivesAndAcknowledgesMessageForIssue52() throws Exception {
        try (NatsBinderTestServer ts = new NatsBinderTestServer(new String[]{"-js"}, false)) {
            this.contextRunner.withPropertyValues("nats.spring.server=" + ts.getURI()).run(context -> {
                Connection conn = context.getBean(Connection.class);
                assertConnected(conn, ts.getURI());

                try (BinderFixture fixture = newGlobalBinder(ts.getURI())) {
                    fixture.binder().setApplicationContext(context.getSourceApplicationContext(GenericApplicationContext.class));
                    String stream = uniqueNatsName("JS_POLLED");
                    String durable = uniqueNatsName("js_polled");
                    String subject = uniqueSubject("jetstream.polled.issue52");
                    String payload = "poll then ack";
                    addMemoryStream(conn, stream, subject);
                    addConsumer(conn, stream, durable, subject);

                    JetStream js = conn.jetStream();
                    js.publish(subject, headersWithTrace("js-polled"), payload.getBytes(UTF_8));

                    DefaultPollableMessageSource source = new DefaultPollableMessageSource(null);
                    AtomicReference<org.springframework.messaging.Message<?>> received = new AtomicReference<>();
                    ExtendedConsumerProperties<NatsConsumerProperties> consumerProperties =
                            new ExtendedConsumerProperties<>(new NatsConsumerProperties());
                    consumerProperties.getExtension().setJetStream(true);
                    consumerProperties.getExtension().setStreamName(stream);
                    consumerProperties.getExtension().setConsumerName(durable);
                    Binding<?> consumerBinding = null;

                    try {
                        consumerBinding = fixture.binder().bindPollableConsumer(subject, "", source, consumerProperties);
                        source.start();

                        assertThat(source.poll(received::set)).isTrue();
                        org.springframework.messaging.Message<?> message = received.get();
                        assertThat(message).isNotNull();
                        assertThat(payloadText(message.getPayload())).isEqualTo(payload);
                        assertThat(message.getHeaders()).containsEntry(TRACE_HEADER, "js-polled");

                        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
                                assertThat(consumerInfo(conn, stream, durable).getNumAckPending()).isZero());
                    } finally {
                        source.stop();
                        if (consumerBinding != null) {
                            consumerBinding.unbind();
                        }
                    }
                }
            });
        }
    }

    @Test
    void jetStreamPolledConsumerReceiveReturnsNullWhenStoppedForIssue52() throws Exception {
        try (NatsBinderTestServer ts = new NatsBinderTestServer(new String[]{"-js"}, false)) {
            this.contextRunner.withPropertyValues("nats.spring.server=" + ts.getURI()).run(context -> {
                Connection conn = context.getBean(Connection.class);
                assertConnected(conn, ts.getURI());

                try (BinderFixture fixture = newGlobalBinder(ts.getURI())) {
                    String stream = uniqueNatsName("JS_POLLED_STOPPED");
                    String consumer = uniqueNatsName("js_polled_stopped");
                    String subject = uniqueSubject("jetstream.polled.stopped.issue52");
                    addMemoryStream(conn, stream, subject);
                    addConsumer(conn, stream, consumer, subject);
                    NatsConsumerDestination from = (NatsConsumerDestination) fixture.provisioner()
                            .provisionConsumerDestination(subject, "", null);
                    NatsMessageSource source = new NatsMessageSource(
                            from,
                            fixture.connection(),
                            true,
                            true,
                            true,
                            stream,
                            consumer);

                    assertThat(source.receive()).isNull();
                }
            });
        }
    }

    @Test
    void jetStreamPolledConsumerStopUnblocksIdleReceiveForIssue52() throws Exception {
        try (NatsBinderTestServer ts = new NatsBinderTestServer(new String[]{"-js"}, false)) {
            this.contextRunner.withPropertyValues("nats.spring.server=" + ts.getURI()).run(context -> {
                Connection conn = context.getBean(Connection.class);
                assertConnected(conn, ts.getURI());

                try (BinderFixture fixture = newGlobalBinder(ts.getURI())) {
                    String stream = uniqueNatsName("JS_POLLED_STOP_IDLE");
                    String consumer = uniqueNatsName("js_polled_stop_idle");
                    String subject = uniqueSubject("jetstream.polled.stop.idle.issue52");
                    addMemoryStream(conn, stream, subject);
                    addConsumer(conn, stream, consumer, subject);
                    NatsConsumerDestination from = (NatsConsumerDestination) fixture.provisioner()
                            .provisionConsumerDestination(subject, "", null);
                    NatsMessageSource source = new NatsMessageSource(
                            from,
                            fixture.connection(),
                            true,
                            true,
                            true,
                            stream,
                            consumer);

                    try {
                        source.start();
                        CompletableFuture<org.springframework.messaging.Message<Object>> receive =
                                CompletableFuture.supplyAsync(source::receive);
                        Thread.sleep(100);
                        assertThat(receive).isNotDone();

                        source.stop();

                        assertThat(receive.get(500, TimeUnit.MILLISECONDS)).isNull();
                    } finally {
                        source.stop();
                    }
                }
            });
        }
    }

    @Test
    void jetStreamPolledConsumerAcknowledgmentCallbackIgnoresRepeatForIssue52() throws Exception {
        try (NatsBinderTestServer ts = new NatsBinderTestServer(new String[]{"-js"}, false)) {
            this.contextRunner.withPropertyValues("nats.spring.server=" + ts.getURI()).run(context -> {
                Connection conn = context.getBean(Connection.class);
                assertConnected(conn, ts.getURI());

                try (BinderFixture fixture = newGlobalBinder(ts.getURI())) {
                    String stream = uniqueNatsName("JS_POLLED_REPEAT_ACK");
                    String consumer = uniqueNatsName("js_polled_repeat_ack");
                    String subject = uniqueSubject("jetstream.polled.repeat.ack.issue52");
                    String payload = "ack once";
                    addMemoryStream(conn, stream, subject);
                    addConsumer(conn, stream, consumer, subject);
                    NatsConsumerDestination from = (NatsConsumerDestination) fixture.provisioner()
                            .provisionConsumerDestination(subject, "", null);
                    NatsMessageSource source = new NatsMessageSource(
                            from,
                            fixture.connection(),
                            true,
                            true,
                            true,
                            stream,
                            consumer);

                    try {
                        source.start();
                        conn.jetStream().publish(subject, payload.getBytes(UTF_8));

                        org.springframework.messaging.Message<Object> message = source.receive();
                        assertThat(message).isNotNull();
                        assertThat(payloadText(message.getPayload())).isEqualTo(payload);
                        AcknowledgmentCallback callback = message.getHeaders().get(
                                IntegrationMessageHeaderAccessor.ACKNOWLEDGMENT_CALLBACK,
                                AcknowledgmentCallback.class);
                        assertThat(callback).isNotNull();
                        assertThat(callback.isAcknowledged()).isFalse();

                        callback.acknowledge(AcknowledgmentCallback.Status.ACCEPT);
                        callback.acknowledge(AcknowledgmentCallback.Status.ACCEPT);

                        assertThat(callback.isAcknowledged()).isTrue();
                        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
                                assertThat(consumerInfo(conn, stream, consumer).getNumAckPending()).isZero());
                    } finally {
                        source.stop();
                    }
                }
            });
        }
    }

    @Test
    void jetStreamPolledConsumerReturnsFalseWhenNoMessageIsAvailableForIssue52() throws Exception {
        try (NatsBinderTestServer ts = new NatsBinderTestServer(new String[]{"-js"}, false)) {
            this.contextRunner.withPropertyValues("nats.spring.server=" + ts.getURI()).run(context -> {
                Connection conn = context.getBean(Connection.class);
                assertConnected(conn, ts.getURI());

                try (BinderFixture fixture = newGlobalBinder(ts.getURI())) {
                    fixture.binder().setApplicationContext(context.getSourceApplicationContext(GenericApplicationContext.class));
                    String stream = uniqueNatsName("JS_POLLED_EMPTY");
                    String durable = uniqueNatsName("js_polled_empty");
                    String subject = uniqueSubject("jetstream.polled.empty.issue52");
                    addMemoryStream(conn, stream, subject);
                    addConsumer(conn, stream, durable, subject);

                    DefaultPollableMessageSource source = new DefaultPollableMessageSource(null);
                    ExtendedConsumerProperties<NatsConsumerProperties> consumerProperties =
                            new ExtendedConsumerProperties<>(new NatsConsumerProperties());
                    consumerProperties.getExtension().setJetStream(true);
                    consumerProperties.getExtension().setStreamName(stream);
                    consumerProperties.getExtension().setConsumerName(durable);
                    Binding<?> consumerBinding = null;

                    try {
                        consumerBinding = fixture.binder().bindPollableConsumer(subject, "", source, consumerProperties);
                        source.start();

                        CompletableFuture<Boolean> emptyPoll = CompletableFuture.supplyAsync(() ->
                                source.poll(message -> {
                                    throw new AssertionError("unexpected message");
                                }));
                        assertThat(emptyPoll.get(2, TimeUnit.SECONDS)).isFalse();
                    } finally {
                        source.stop();
                        if (consumerBinding != null) {
                            consumerBinding.unbind();
                        }
                    }
                }
            });
        }
    }

    @Test
    void jetStreamPolledConsumerRequiresStreamAndConsumerIdentityForIssue52() throws Exception {
        try (NatsBinderTestServer ts = new NatsBinderTestServer(new String[]{"-js"}, false)) {
            this.contextRunner.withPropertyValues("nats.spring.server=" + ts.getURI()).run(context -> {
                Connection conn = context.getBean(Connection.class);
                assertConnected(conn, ts.getURI());

                try (BinderFixture fixture = newGlobalBinder(ts.getURI())) {
                    fixture.binder().setApplicationContext(context.getSourceApplicationContext(GenericApplicationContext.class));
                    String stream = uniqueNatsName("JS_POLLED_IDENTITY");
                    String consumer = uniqueNatsName("js_polled_identity");
                    String subject = uniqueSubject("jetstream.polled.identity.issue52");

                    DefaultPollableMessageSource missingStreamSource = new DefaultPollableMessageSource(null);
                    ExtendedConsumerProperties<NatsConsumerProperties> missingStreamProperties =
                            new ExtendedConsumerProperties<>(new NatsConsumerProperties());
                    missingStreamProperties.getExtension().setJetStream(true);
                    missingStreamProperties.getExtension().setConsumerName(consumer);

                    assertThatThrownBy(() -> fixture.binder().bindPollableConsumer(subject, "",
                            missingStreamSource, missingStreamProperties))
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("NATS JetStream polled consumers require stream-name");

                    addMemoryStream(conn, stream, subject);
                    DefaultPollableMessageSource missingConsumerSource = new DefaultPollableMessageSource(null);
                    ExtendedConsumerProperties<NatsConsumerProperties> missingConsumerProperties =
                            new ExtendedConsumerProperties<>(new NatsConsumerProperties());
                    missingConsumerProperties.getExtension().setJetStream(true);
                    missingConsumerProperties.getExtension().setStreamName(stream);

                    assertThatThrownBy(() -> fixture.binder().bindPollableConsumer(subject, "",
                            missingConsumerSource, missingConsumerProperties))
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("NATS JetStream polled consumers require consumer-name or a consumer group");
                }
            });
        }
    }

    @Test
    void jetStreamPolledConsumerRequeuesWhenHandlerRequestsRedeliveryForIssue52() throws Exception {
        try (NatsBinderTestServer ts = new NatsBinderTestServer(new String[]{"-js"}, false)) {
            this.contextRunner.withPropertyValues("nats.spring.server=" + ts.getURI()).run(context -> {
                Connection conn = context.getBean(Connection.class);
                assertConnected(conn, ts.getURI());

                try (BinderFixture fixture = newGlobalBinder(ts.getURI())) {
                    fixture.binder().setApplicationContext(context.getSourceApplicationContext(GenericApplicationContext.class));
                    String stream = uniqueNatsName("JS_POLLED_REQUEUE");
                    String durable = uniqueNatsName("js_polled_requeue");
                    String subject = uniqueSubject("jetstream.polled.requeue.issue52");
                    String payload = "requeue me";
                    addMemoryStream(conn, stream, subject);
                    addConsumer(conn, stream, durable, subject);

                    DefaultPollableMessageSource source = new DefaultPollableMessageSource(null);
                    AtomicReference<org.springframework.messaging.Message<?>> redelivered = new AtomicReference<>();
                    ExtendedConsumerProperties<NatsConsumerProperties> consumerProperties =
                            new ExtendedConsumerProperties<>(new NatsConsumerProperties());
                    consumerProperties.getExtension().setJetStream(true);
                    consumerProperties.getExtension().setStreamName(stream);
                    consumerProperties.getExtension().setConsumerName(durable);
                    Binding<?> consumerBinding = null;

                    try {
                        consumerBinding = fixture.binder().bindPollableConsumer(subject, "", source, consumerProperties);
                        source.start();

                        conn.jetStream().publish(subject, payload.getBytes(UTF_8));

                        assertThat(source.poll(message -> {
                            assertThat(payloadText(message.getPayload())).isEqualTo(payload);
                            throw new RequeueCurrentMessageException("redeliver");
                        })).isTrue();

                        CompletableFuture<Boolean> secondPoll =
                                CompletableFuture.supplyAsync(() -> source.poll(redelivered::set));
                        assertThat(secondPoll.get(5, TimeUnit.SECONDS)).isTrue();
                        assertThat(payloadText(redelivered.get().getPayload())).isEqualTo(payload);

                        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
                                assertThat(consumerInfo(conn, stream, durable).getNumAckPending()).isZero());
                    } finally {
                        source.stop();
                        if (consumerBinding != null) {
                            consumerBinding.unbind();
                        }
                    }
                }
            });
        }
    }

    @Test
    void jetStreamPolledConsumerNacksWhenHandlerThrowsForIssue52() throws Exception {
        try (NatsBinderTestServer ts = new NatsBinderTestServer(new String[]{"-js"}, false)) {
            this.contextRunner.withPropertyValues("nats.spring.server=" + ts.getURI()).run(context -> {
                Connection conn = context.getBean(Connection.class);
                assertConnected(conn, ts.getURI());

                try (BinderFixture fixture = newGlobalBinder(ts.getURI())) {
                    fixture.binder().setApplicationContext(context.getSourceApplicationContext(GenericApplicationContext.class));
                    String stream = uniqueNatsName("JS_POLLED_EXCEPTION");
                    String durable = uniqueNatsName("js_polled_exception");
                    String subject = uniqueSubject("jetstream.polled.exception.issue52");
                    String payload = "retry after exception";
                    addMemoryStream(conn, stream, subject);
                    addConsumer(conn, stream, durable, subject);

                    DefaultPollableMessageSource source = new DefaultPollableMessageSource(null);
                    AtomicReference<org.springframework.messaging.Message<?>> redelivered = new AtomicReference<>();
                    ExtendedConsumerProperties<NatsConsumerProperties> consumerProperties =
                            new ExtendedConsumerProperties<>(new NatsConsumerProperties());
                    consumerProperties.getExtension().setJetStream(true);
                    consumerProperties.getExtension().setStreamName(stream);
                    consumerProperties.getExtension().setConsumerName(durable);
                    Binding<?> consumerBinding = null;

                    try {
                        consumerBinding = fixture.binder().bindPollableConsumer(subject, "", source, consumerProperties);
                        source.start();

                        conn.jetStream().publish(subject, payload.getBytes(UTF_8));

                        assertThatThrownBy(() -> source.poll(message -> {
                            assertThat(payloadText(message.getPayload())).isEqualTo(payload);
                            throw new IllegalStateException("transient");
                        })).isInstanceOf(MessageHandlingException.class);

                        CompletableFuture<Boolean> secondPoll =
                                CompletableFuture.supplyAsync(() -> source.poll(redelivered::set));
                        assertThat(secondPoll.get(5, TimeUnit.SECONDS)).isTrue();
                        assertThat(payloadText(redelivered.get().getPayload())).isEqualTo(payload);

                        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
                                assertThat(consumerInfo(conn, stream, durable).getNumAckPending()).isZero());
                    } finally {
                        source.stop();
                        if (consumerBinding != null) {
                            consumerBinding.unbind();
                        }
                    }
                }
            });
        }
    }

    @Test
    void jetStreamPolledConsumerUsesGroupAsConsumerNameWhenConsumerNameIsUnsetForIssue52() throws Exception {
        try (NatsBinderTestServer ts = new NatsBinderTestServer(new String[]{"-js"}, false)) {
            this.contextRunner.withPropertyValues("nats.spring.server=" + ts.getURI()).run(context -> {
                Connection conn = context.getBean(Connection.class);
                assertConnected(conn, ts.getURI());

                try (BinderFixture fixture = newGlobalBinder(ts.getURI())) {
                    fixture.binder().setApplicationContext(context.getSourceApplicationContext(GenericApplicationContext.class));
                    String stream = uniqueNatsName("JS_POLLED_GROUP");
                    String group = uniqueNatsName("js_polled_group");
                    String subject = uniqueSubject("jetstream.polled.group.issue52");
                    String payload = "group consumer";
                    addMemoryStream(conn, stream, subject);
                    addConsumer(conn, stream, group, subject);

                    DefaultPollableMessageSource source = new DefaultPollableMessageSource(null);
                    AtomicReference<org.springframework.messaging.Message<?>> received = new AtomicReference<>();
                    ExtendedConsumerProperties<NatsConsumerProperties> consumerProperties =
                            new ExtendedConsumerProperties<>(new NatsConsumerProperties());
                    consumerProperties.getExtension().setJetStream(true);
                    consumerProperties.getExtension().setStreamName(stream);
                    Binding<?> consumerBinding = null;

                    try {
                        consumerBinding = fixture.binder().bindPollableConsumer(subject, group, source, consumerProperties);
                        source.start();

                        conn.jetStream().publish(subject, payload.getBytes(UTF_8));

                        assertThat(source.poll(received::set)).isTrue();
                        assertThat(payloadText(received.get().getPayload())).isEqualTo(payload);

                        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
                                assertThat(consumerInfo(conn, stream, group).getNumAckPending()).isZero());
                    } finally {
                        source.stop();
                        if (consumerBinding != null) {
                            consumerBinding.unbind();
                        }
                    }
                }
            });
        }
    }

    @Test
    void jetStreamPolledConsumerUsesNamedConsumerContextForIssue52() throws Exception {
        try (NatsBinderTestServer ts = new NatsBinderTestServer(new String[]{"-js"}, false)) {
            this.contextRunner.withPropertyValues("nats.spring.server=" + ts.getURI()).run(context -> {
                Connection conn = context.getBean(Connection.class);
                assertConnected(conn, ts.getURI());

                try (BinderFixture fixture = newGlobalBinder(ts.getURI())) {
                    String stream = uniqueNatsName("JS_POLLED_CONTEXT");
                    String consumer = uniqueNatsName("js_polled_context");
                    String subject = uniqueSubject("jetstream.polled.context.issue52");
                    String payload = "poll named consumer";
                    addMemoryStream(conn, stream, subject);
                    addConsumer(conn, stream, consumer, subject);

                    NatsConsumerDestination from = (NatsConsumerDestination) fixture.provisioner()
                            .provisionConsumerDestination(subject, "", null);
                    NatsMessageSource src = new NatsMessageSource(
                            from,
                            fixture.connection(),
                            true,
                            true,
                            true,
                            stream,
                            consumer);

                    try {
                        src.start();
                        CompletableFuture<org.springframework.messaging.Message<Object>> received =
                                CompletableFuture.supplyAsync(src::receive);

                        conn.jetStream().publish(subject, payload.getBytes(UTF_8));

                        org.springframework.messaging.Message<Object> message = received.get(5, TimeUnit.SECONDS);
                        assertThat(payloadText(message.getPayload())).isEqualTo(payload);
                    } finally {
                        src.stop();
                    }
                }
            });
        }
    }

    @Test
    void jetStreamProducerRejectsReplyChannelForIssue52() throws Exception {
        try (NatsBinderTestServer ts = new NatsBinderTestServer(new String[]{"-js"}, false)) {
            this.contextRunner.withPropertyValues("nats.spring.server=" + ts.getURI()).run(context -> {
                Connection conn = context.getBean(Connection.class);
                assertConnected(conn, ts.getURI());

                try (BinderFixture fixture = newGlobalBinder(ts.getURI())) {
                    String stream = uniqueNatsName("JS_REPLY");
                    String subject = uniqueSubject("jetstream.reply.issue52");
                    addMemoryStream(conn, stream, subject);

                    ExtendedProducerProperties<NatsProducerProperties> producerProperties =
                            new ExtendedProducerProperties<>(new NatsProducerProperties());
                    producerProperties.getExtension().setJetStream(true);
                    producerProperties.getExtension().setStreamName(stream);
                    ProducerDestination to = fixture.provisioner().provisionProducerDestination(subject, null);
                    MessageHandler handler = fixture.binder().createProducerMessageHandler(to, producerProperties, null);

                    assertThatThrownBy(() -> handler.handleMessage(MessageBuilder.withPayload("request")
                            .setHeader(MessageHeaders.REPLY_CHANNEL, "reply.subject")
                            .build()))
                            .isInstanceOf(MessageHandlingException.class)
                            .hasMessageContaining("JetStream publishing does not support reply channels");
                }
            });
        }
    }

    @Test
    void jetStreamProducerReportsServerPublishFailureForIssue52() throws Exception {
        try (NatsBinderTestServer ts = new NatsBinderTestServer(new String[]{"-js"}, false)) {
            this.contextRunner.withPropertyValues("nats.spring.server=" + ts.getURI()).run(context -> {
                Connection conn = context.getBean(Connection.class);
                assertConnected(conn, ts.getURI());

                try (BinderFixture fixture = newGlobalBinder(ts.getURI())) {
                    String stream = uniqueNatsName("JS_MISSING_PUBLISH");
                    String subject = uniqueSubject("jetstream.missing.publish.issue52");
                    ExtendedProducerProperties<NatsProducerProperties> producerProperties =
                            new ExtendedProducerProperties<>(new NatsProducerProperties());
                    producerProperties.getExtension().setJetStream(true);
                    producerProperties.getExtension().setStreamName(stream);
                    ProducerDestination to = fixture.provisioner().provisionProducerDestination(subject, null);
                    MessageHandler handler = fixture.binder().createProducerMessageHandler(to, producerProperties, null);

                    assertThatThrownBy(() -> handler.handleMessage(MessageBuilder.withPayload("missing stream").build()))
                            .isInstanceOf(MessageHandlingException.class)
                            .hasMessageContaining("Failed to publish message to NATS JetStream subject " + subject);
                }
            });
        }
    }

    @Test
    void embeddedHeaderModeEmbedsStandardHeadersInPayloadForIssue13() throws Exception {
        try (NatsBinderTestServer ts = new NatsBinderTestServer()) {
            this.contextRunner.withPropertyValues("nats.spring.server=" + ts.getURI()).run(context -> {
                Connection conn = context.getBean(Connection.class);
                assertConnected(conn, ts.getURI());

                try (BinderFixture fixture = newGlobalBinder(ts.getURI())) {
                    fixture.binder().setApplicationContext(context.getSourceApplicationContext(GenericApplicationContext.class));
                    String subject = "binder.embedded.headers.issue13.raw";
                    String payload = "embedded header payload";
                    DirectChannel output = new DirectChannel();
                    ExtendedProducerProperties<NatsProducerProperties> producerProperties =
                            new ExtendedProducerProperties<>(new NatsProducerProperties());
                    producerProperties.setHeaderMode(HeaderMode.embeddedHeaders);
                    Binding<MessageChannel> producerBinding = null;
                    Subscription sub = conn.subscribe(subject);

                    try {
                        producerBinding = fixture.binder().bindProducer(subject, output, producerProperties);
                        conn.flush(FLUSH_TIMEOUT);

                        output.send(MessageBuilder.withPayload(payload.getBytes(UTF_8))
                                .setHeader(MessageHeaders.CONTENT_TYPE, "text/plain")
                                .build());
                        fixture.connection().flush(FLUSH_TIMEOUT);

                        Message raw = sub.nextMessage(FLUSH_TIMEOUT);
                        assertThat(raw).isNotNull();
                        assertThat(raw.hasHeaders()).isFalse();
                        assertThat(EmbeddedHeaderUtils.mayHaveEmbeddedHeaders(raw.getData())).isTrue();

                        MessageValues extracted = EmbeddedHeaderUtils.extractHeaders(raw.getData());
                        assertThat(payloadText(extracted.getPayload())).isEqualTo(payload);
                        assertThat(extracted.get(MessageHeaders.CONTENT_TYPE)).isEqualTo("text/plain");
                    } finally {
                        if (producerBinding != null) {
                            producerBinding.unbind();
                        }
                    }
                }
            });
        }
    }

    @Test
    void embeddedHeaderModeEmbedsConfiguredCustomHeadersForIssue13() throws Exception {
        try (NatsBinderTestServer ts = new NatsBinderTestServer()) {
            this.contextRunner.withPropertyValues("nats.spring.server=" + ts.getURI()).run(context -> {
                Connection conn = context.getBean(Connection.class);
                assertConnected(conn, ts.getURI());

                try (BinderFixture fixture = newGlobalBinder(ts.getURI(), TRACE_HEADER)) {
                    fixture.binder().setApplicationContext(context.getSourceApplicationContext(GenericApplicationContext.class));
                    String subject = "binder.embedded.headers.issue13.custom";
                    String payload = "embedded custom header payload";
                    DirectChannel output = new DirectChannel();
                    ExtendedProducerProperties<NatsProducerProperties> producerProperties =
                            new ExtendedProducerProperties<>(new NatsProducerProperties());
                    producerProperties.setHeaderMode(HeaderMode.embeddedHeaders);
                    Binding<MessageChannel> producerBinding = null;
                    Subscription sub = conn.subscribe(subject);

                    try {
                        producerBinding = fixture.binder().bindProducer(subject, output, producerProperties);
                        conn.flush(FLUSH_TIMEOUT);

                        output.send(MessageBuilder.withPayload(payload.getBytes(UTF_8))
                                .setHeader(MessageHeaders.CONTENT_TYPE, "text/plain")
                                .setHeader(TRACE_HEADER, "trace-embedded")
                                .build());
                        fixture.connection().flush(FLUSH_TIMEOUT);

                        Message raw = sub.nextMessage(FLUSH_TIMEOUT);
                        assertThat(raw).isNotNull();
                        assertThat(raw.hasHeaders()).isFalse();

                        MessageValues extracted = EmbeddedHeaderUtils.extractHeaders(raw.getData());
                        assertThat(payloadText(extracted.getPayload())).isEqualTo(payload);
                        assertThat(extracted.get(MessageHeaders.CONTENT_TYPE)).isEqualTo("text/plain");
                        assertThat(extracted.get(TRACE_HEADER)).isEqualTo("trace-embedded");
                    } finally {
                        if (producerBinding != null) {
                            producerBinding.unbind();
                        }
                    }
                }
            });
        }
    }

    @Test
    void embeddedHeaderModeRoundTripsThroughProducerAndConsumerBindingsForIssue13() throws Exception {
        try (NatsBinderTestServer ts = new NatsBinderTestServer()) {
            this.contextRunner.withPropertyValues("nats.spring.server=" + ts.getURI()).run(context -> {
                Connection conn = context.getBean(Connection.class);
                assertConnected(conn, ts.getURI());

                try (BinderFixture fixture = newGlobalBinder(ts.getURI())) {
                    fixture.binder().setApplicationContext(context.getSourceApplicationContext(GenericApplicationContext.class));
                    String subject = "binder.embedded.headers.issue13.roundtrip";
                    String payload = "embedded header round trip";
                    DirectChannel output = new DirectChannel();
                    DirectChannel input = new DirectChannel();
                    CompletableFuture<org.springframework.messaging.Message<?>> received = new CompletableFuture<>();
                    input.subscribe(received::complete);
                    ExtendedProducerProperties<NatsProducerProperties> producerProperties =
                            new ExtendedProducerProperties<>(new NatsProducerProperties());
                    producerProperties.setHeaderMode(HeaderMode.embeddedHeaders);
                    ExtendedConsumerProperties<NatsConsumerProperties> consumerProperties =
                            new ExtendedConsumerProperties<>(new NatsConsumerProperties());
                    consumerProperties.setHeaderMode(HeaderMode.embeddedHeaders);
                    Binding<MessageChannel> producerBinding = null;
                    Binding<MessageChannel> consumerBinding = null;

                    try {
                        consumerBinding = fixture.binder().bindConsumer(subject, "", input, consumerProperties);
                        producerBinding = fixture.binder().bindProducer(subject, output, producerProperties);
                        fixture.connection().flush(FLUSH_TIMEOUT);

                        output.send(MessageBuilder.withPayload(payload.getBytes(UTF_8))
                                .setHeader(MessageHeaders.CONTENT_TYPE, "text/plain")
                                .build());
                        fixture.connection().flush(FLUSH_TIMEOUT);

                        org.springframework.messaging.Message<?> message = received.get(5, TimeUnit.SECONDS);
                        assertThat(payloadText(message.getPayload())).isEqualTo(payload);
                        assertThat(message.getHeaders().get(MessageHeaders.CONTENT_TYPE)).isEqualTo("text/plain");
                    } finally {
                        if (producerBinding != null) {
                            producerBinding.unbind();
                        }
                        if (consumerBinding != null) {
                            consumerBinding.unbind();
                        }
                    }
                }
            });
        }
    }

    @Test
    void embeddedHeaderModeRestoresStandardHeadersFromPayloadForIssue13() throws Exception {
        try (NatsBinderTestServer ts = new NatsBinderTestServer()) {
            this.contextRunner.withPropertyValues("nats.spring.server=" + ts.getURI()).run(context -> {
                Connection conn = context.getBean(Connection.class);
                assertConnected(conn, ts.getURI());

                try (BinderFixture fixture = newGlobalBinder(ts.getURI())) {
                    fixture.binder().setApplicationContext(context.getSourceApplicationContext(GenericApplicationContext.class));
                    String subject = "binder.embedded.headers.issue13.consumer";
                    String payload = "embedded header consumer";
                    DirectChannel input = new DirectChannel();
                    CompletableFuture<org.springframework.messaging.Message<?>> received = new CompletableFuture<>();
                    input.subscribe(received::complete);
                    ExtendedConsumerProperties<NatsConsumerProperties> consumerProperties =
                            new ExtendedConsumerProperties<>(new NatsConsumerProperties());
                    consumerProperties.setHeaderMode(HeaderMode.embeddedHeaders);
                    Binding<MessageChannel> consumerBinding = null;

                    try {
                        consumerBinding = fixture.binder().bindConsumer(subject, "", input, consumerProperties);
                        fixture.connection().flush(FLUSH_TIMEOUT);

                        MessageValues values = new MessageValues(payload.getBytes(UTF_8), Map.of(
                                MessageHeaders.CONTENT_TYPE, "text/plain"
                        ));
                        byte[] embeddedPayload = EmbeddedHeaderUtils.embedHeaders(
                                values,
                                EmbeddedHeaderUtils.headersToEmbed(null)
                        );
                        conn.publish(subject, embeddedPayload);
                        conn.flush(FLUSH_TIMEOUT);

                        org.springframework.messaging.Message<?> message = received.get(5, TimeUnit.SECONDS);
                        assertThat(payloadText(message.getPayload())).isEqualTo(payload);
                        assertThat(message.getHeaders().get(MessageHeaders.CONTENT_TYPE)).isEqualTo("text/plain");
                    } finally {
                        if (consumerBinding != null) {
                            consumerBinding.unbind();
                        }
                    }
                }
            });
        }
    }

    @Test
    void headerModeNoneDoesNotPublishNativeNatsHeaders() throws Exception {
        try (NatsBinderTestServer ts = new NatsBinderTestServer()) {
            this.contextRunner.withPropertyValues("nats.spring.server=" + ts.getURI()).run(context -> {
                Connection conn = context.getBean(Connection.class);
                assertConnected(conn, ts.getURI());

                try (BinderFixture fixture = newGlobalBinder(ts.getURI())) {
                    fixture.binder().setApplicationContext(context.getSourceApplicationContext(GenericApplicationContext.class));
                    String subject = "binder.headers.none.producer";
                    String payload = "headers suppressed";
                    DirectChannel output = new DirectChannel();
                    ExtendedProducerProperties<NatsProducerProperties> producerProperties =
                            new ExtendedProducerProperties<>(new NatsProducerProperties());
                    producerProperties.setHeaderMode(HeaderMode.none);
                    Binding<MessageChannel> producerBinding = null;
                    Subscription sub = conn.subscribe(subject);

                    try {
                        producerBinding = fixture.binder().bindProducer(subject, output, producerProperties);
                        conn.flush(FLUSH_TIMEOUT);

                        output.send(MessageBuilder.withPayload(payload.getBytes(UTF_8))
                                .setHeader(TRACE_HEADER, "suppressed")
                                .build());
                        fixture.connection().flush(FLUSH_TIMEOUT);

                        Message raw = sub.nextMessage(FLUSH_TIMEOUT);
                        assertThat(raw).isNotNull();
                        assertThat(new String(raw.getData(), UTF_8)).isEqualTo(payload);
                        assertThat(raw.hasHeaders()).isFalse();
                    } finally {
                        if (producerBinding != null) {
                            producerBinding.unbind();
                        }
                    }
                }
            });
        }
    }

    @Test
    void headerModeNoneDoesNotExposeNativeNatsHeadersToConsumer() throws Exception {
        try (NatsBinderTestServer ts = new NatsBinderTestServer()) {
            this.contextRunner.withPropertyValues("nats.spring.server=" + ts.getURI()).run(context -> {
                Connection conn = context.getBean(Connection.class);
                assertConnected(conn, ts.getURI());

                try (BinderFixture fixture = newGlobalBinder(ts.getURI())) {
                    fixture.binder().setApplicationContext(context.getSourceApplicationContext(GenericApplicationContext.class));
                    String subject = "binder.headers.none.consumer";
                    String payload = "headers hidden";
                    DirectChannel input = new DirectChannel();
                    CompletableFuture<org.springframework.messaging.Message<?>> received = new CompletableFuture<>();
                    input.subscribe(received::complete);
                    ExtendedConsumerProperties<NatsConsumerProperties> consumerProperties =
                            new ExtendedConsumerProperties<>(new NatsConsumerProperties());
                    consumerProperties.setHeaderMode(HeaderMode.none);
                    Binding<MessageChannel> consumerBinding = null;

                    try {
                        consumerBinding = fixture.binder().bindConsumer(subject, "", input, consumerProperties);
                        fixture.connection().flush(FLUSH_TIMEOUT);

                        conn.publish(subject, "actual.reply", headersWithTrace("hidden"), payload.getBytes(UTF_8));
                        conn.flush(FLUSH_TIMEOUT);

                        org.springframework.messaging.Message<?> message = received.get(5, TimeUnit.SECONDS);
                        assertThat(payloadText(message.getPayload())).isEqualTo(payload);
                        assertThat(message.getHeaders()).doesNotContainKey(TRACE_HEADER);
                        assertThat(message.getHeaders()).doesNotContainKey(BinderHeaders.NATIVE_HEADERS_PRESENT);
                        assertThat(message.getHeaders()).containsEntry(NatsMessageProducer.SUBJECT, subject);
                        assertThat(message.getHeaders()).containsEntry(MessageHeaders.REPLY_CHANNEL, "actual.reply");
                    } finally {
                        if (consumerBinding != null) {
                            consumerBinding.unbind();
                        }
                    }
                }
            });
        }
    }

    @Test
    void embeddedHeaderModeDoesNotParsePayloadWhenNativeNatsHeadersArePresent() throws Exception {
        try (NatsBinderTestServer ts = new NatsBinderTestServer()) {
            this.contextRunner.withPropertyValues("nats.spring.server=" + ts.getURI()).run(context -> {
                Connection conn = context.getBean(Connection.class);
                assertConnected(conn, ts.getURI());

                try (BinderFixture fixture = newGlobalBinder(ts.getURI())) {
                    fixture.binder().setApplicationContext(context.getSourceApplicationContext(GenericApplicationContext.class));
                    String subject = "binder.embedded.headers.native.present";
                    String payload = "payload that should stay embedded";
                    DirectChannel input = new DirectChannel();
                    CompletableFuture<org.springframework.messaging.Message<?>> received = new CompletableFuture<>();
                    input.subscribe(received::complete);
                    ExtendedConsumerProperties<NatsConsumerProperties> consumerProperties =
                            new ExtendedConsumerProperties<>(new NatsConsumerProperties());
                    consumerProperties.setHeaderMode(HeaderMode.embeddedHeaders);
                    Binding<MessageChannel> consumerBinding = null;

                    try {
                        consumerBinding = fixture.binder().bindConsumer(subject, "", input, consumerProperties);
                        fixture.connection().flush(FLUSH_TIMEOUT);

                        MessageValues values = new MessageValues(payload.getBytes(UTF_8), Map.of(
                                MessageHeaders.CONTENT_TYPE, "text/plain"
                        ));
                        byte[] embeddedPayload = EmbeddedHeaderUtils.embedHeaders(
                                values,
                                EmbeddedHeaderUtils.headersToEmbed(null)
                        );
                        conn.publish(subject, headersWithTrace("native"), embeddedPayload);
                        conn.flush(FLUSH_TIMEOUT);

                        org.springframework.messaging.Message<?> message = received.get(5, TimeUnit.SECONDS);
                        assertThat(message.getPayload()).isEqualTo(embeddedPayload);
                        assertThat(message.getHeaders()).containsEntry(BinderHeaders.NATIVE_HEADERS_PRESENT, true);
                        assertThat(message.getHeaders()).doesNotContainKey(TRACE_HEADER);
                        assertThat(message.getHeaders()).doesNotContainKey(MessageHeaders.CONTENT_TYPE);
                    } finally {
                        if (consumerBinding != null) {
                            consumerBinding.unbind();
                        }
                    }
                }
            });
        }
    }

    @Test
    void testRequestReply() throws Exception {
        try (NatsBinderTestServer ts = new NatsBinderTestServer()) {
            this.contextRunner.withPropertyValues("nats.spring.server=" + ts.getURI()).run(context -> {
                Connection conn = context.getBean(Connection.class);
                assertConnected(conn, ts.getURI());

                try (BinderFixture fixture = newGlobalBinder(ts.getURI())) {
                    String request = "hello request";
                    String reply = "hello reply";
                    String req2rep = "req2rep";
                    String rep2req = "rep2req";
                    MessageHandler appOneMh =
                            fixture.binder().createProducerMessageHandler(
                                    fixture.provisioner().provisionProducerDestination(req2rep, null), null, null);
                    CompletableFuture<org.springframework.messaging.Message<?>> appOneReceived = new CompletableFuture<>();
                    NatsMessageProducer appOneMp = buildMessageProducer(rep2req, appOneReceived, fixture);

                    MessageHandler appTwoMh =
                            fixture.binder().createProducerMessageHandler(
                                    fixture.provisioner().provisionProducerDestination(rep2req, null), null, null);
                    CompletableFuture<org.springframework.messaging.Message<?>> appTwoReceived = new CompletableFuture<>();
                    NatsMessageProducer appTwoMp = buildMessageProducer(req2rep, appTwoReceived, fixture);
                    appTwoReceived.thenAccept(requestMsg -> appTwoMh.handleMessage(
                            MessageBuilder.withPayload(reply + " to " + payloadText(requestMsg.getPayload()))
                                    .copyHeaders(requestMsg.getHeaders())
                                    .build()));

                    try {
                        appOneMp.start();
                        appTwoMp.start();
                        fixture.connection().flush(FLUSH_TIMEOUT);

                        appOneMh.handleMessage(MessageBuilder.withPayload(request)
                                .setHeader(MessageHeaders.REPLY_CHANNEL, rep2req)
                                .build());

                        org.springframework.messaging.Message<?> appTwoMessage = appTwoReceived.get(5, TimeUnit.SECONDS);
                        assertThat(appTwoMessage.getHeaders().get(MessageHeaders.REPLY_CHANNEL)).isEqualTo(rep2req);
                        assertThat(payloadText(appTwoMessage.getPayload())).isEqualTo(request);

                        org.springframework.messaging.Message<?> appOneMessage = appOneReceived.get(5, TimeUnit.SECONDS);
                        assertThat(appOneMessage.getHeaders().get(MessageHeaders.REPLY_CHANNEL)).isEqualTo(rep2req);
                        assertThat(payloadText(appOneMessage.getPayload())).isEqualTo(reply + " to " + request);
                    } finally {
                        appOneMp.stop();
                        appTwoMp.stop();
                    }
                }
            });
        }
    }

    private static NatsMessageProducer buildCountingProducer(ConsumerDestination destination, String group,
                                                            BinderFixture fixture, AtomicInteger counter) {
        NatsMessageProducer producer =
                (NatsMessageProducer) fixture.binder().createConsumerEndpoint(destination, group, null);
        DirectChannel output = new DirectChannel();
        output.subscribe(msg -> counter.incrementAndGet());
        producer.setOutputChannel(output);
        return producer;
    }

    private static NatsMessageProducer buildMessageProducer(String subject,
                                                           CompletableFuture<org.springframework.messaging.Message<?>> received,
                                                           BinderFixture fixture) {
        NatsMessageProducer producer = (NatsMessageProducer) fixture.binder().createConsumerEndpoint(
                fixture.provisioner().provisionConsumerDestination(subject, "", null), "", null);
        DirectChannel output = new DirectChannel();
        output.subscribe(received::complete);
        producer.setOutputChannel(output);
        return producer;
    }

    private static Binding<MessageChannel> bindProvisionedJetStreamProducer(BinderFixture fixture,
                                                                            String subject,
                                                                            String stream,
                                                                            StorageType storageType,
                                                                            Integer streamReplicas) {
        DirectChannel output = new DirectChannel();
        ExtendedProducerProperties<NatsProducerProperties> producerProperties =
                new ExtendedProducerProperties<>(new NatsProducerProperties());
        producerProperties.getExtension().setJetStream(true);
        producerProperties.getExtension().setStreamName(stream);
        producerProperties.getExtension().setProvisionStream(true);
        producerProperties.getExtension().setStreamStorageType(storageType);
        producerProperties.getExtension().setStreamReplicas(streamReplicas);
        return fixture.binder().bindProducer(subject, output, producerProperties);
    }

    private static void assertStreamProvisioningFailsForMissingSubject(BinderFixture fixture,
                                                                       String subject,
                                                                       String stream) {
        assertThatThrownBy(() -> bindProvisionedJetStreamProducer(fixture, subject, stream, null, null))
                .isInstanceOf(BinderException.class)
                .satisfies(exp -> assertThat(exp.getCause())
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("does not include subject " + subject));
    }

    private static void bindProvisionedJetStreamProducerAfter(CountDownLatch start,
                                                              BinderFixture fixture,
                                                              String subject,
                                                              String stream,
                                                              AtomicReference<Binding<MessageChannel>> binding,
                                                              AtomicReference<Throwable> failure) {
        try {
            assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
            binding.set(bindProvisionedJetStreamProducer(fixture, subject, stream, null, null));
        } catch (Throwable exp) {
            failure.set(exp);
        }
    }

    private static void addMemoryStream(Connection connection, String stream, String subject)
            throws IOException, JetStreamApiException {
        JetStreamManagement management = connection.jetStreamManagement();
        management.addStream(StreamConfiguration.builder()
                .name(stream)
                .subjects(subject)
                .storageType(StorageType.Memory)
                .build());
    }

    private static ConsumerInfo addConsumer(Connection connection, String stream, String consumer, String subject)
            throws IOException, JetStreamApiException {
        return addConsumer(connection, stream, consumer, subject, ConsumerConfiguration.builder());
    }

    private static ConsumerInfo addConsumer(Connection connection, String stream, String consumer, String subject,
                                            ConsumerConfiguration.Builder builder)
            throws IOException, JetStreamApiException {
        return connection.jetStreamManagement().addOrUpdateConsumer(stream, builder
                .durable(consumer)
                .filterSubject(subject)
                .build());
    }

    private static ConsumerInfo consumerInfo(Connection connection, String stream, String consumer) {
        try {
            return connection.jetStreamManagement().getConsumerInfo(stream, consumer);
        } catch (IOException | JetStreamApiException exp) {
            throw new AssertionError(exp);
        }
    }

    private static io.nats.client.api.StreamInfo streamInfo(Connection connection, String stream) {
        try {
            return connection.jetStreamManagement().getStreamInfo(stream);
        } catch (IOException | JetStreamApiException exp) {
            throw new AssertionError(exp);
        }
    }

    private static String uniqueNatsName(String prefix) {
        return prefix + "_" + System.nanoTime();
    }

    private static String uniqueSubject(String prefix) {
        return prefix + "." + System.nanoTime();
    }

    private static BinderFixture newGlobalBinder(String server) throws IOException, InterruptedException {
        return newGlobalBinder(server, new String[0]);
    }

    private static BinderFixture newGlobalBinder(String server, String... headersToEmbed) throws IOException, InterruptedException {
        NatsProperties natsProperties = new NatsProperties();
        natsProperties.setServer(server);
        NatsBinderConfigurationProperties binderProperties = new NatsBinderConfigurationProperties();
        binderProperties.setHeadersToEmbed(headersToEmbed);
        return newBinder(natsProperties, binderProperties);
    }

    private static BinderFixture newBinderPropertiesBinder(String server) throws IOException, InterruptedException {
        NatsBinderConfigurationProperties binderProperties = new NatsBinderConfigurationProperties();
        binderProperties.setServer(server);
        return newBinder(new NatsProperties(), binderProperties);
    }

    private static BinderFixture newTlsGlobalBinder(String server) throws IOException, InterruptedException {
        NatsProperties natsProperties = new NatsProperties();
        natsProperties.setServer(server);
        natsProperties.setKeyStorePath(SHARED_TLS_RESOURCES + "keystore.jks");
        natsProperties.setKeyStorePassword("password".toCharArray());
        natsProperties.setKeyStoreType("JKS");
        natsProperties.setTrustStorePath(SHARED_TLS_RESOURCES + "cacerts");
        natsProperties.setTrustStorePassword("password".toCharArray());
        natsProperties.setTrustStoreType("JKS");
        return newBinder(natsProperties, new NatsBinderConfigurationProperties());
    }

    private static BinderFixture newBinder(NatsProperties natsProperties,
                                           NatsBinderConfigurationProperties binderProperties)
            throws IOException, InterruptedException {
        NatsExtendedBindingProperties props = new NatsExtendedBindingProperties();
        NatsChannelBinderConfiguration config = new NatsChannelBinderConfiguration(
                null,
                null,
                natsProperties,
                binderProperties,
                props);
        NatsChannelProvisioner provisioner = config.natsChannelProvisioner();

        NatsChannelBinder binder = config.natsBinder(provisioner);
        assertThat(binder).isNotNull();
        assertThat(binder.getConnection()).isNotNull();
        assertThat(binder.getConnection().getStatus()).isSameAs(Connection.Status.CONNECTED);
        return new BinderFixture(provisioner, binder);
    }

    private static void assertConnected(Connection connection, String expectedUrl) {
        assertThat(connection).isNotNull();
        assertThat(connection.getStatus()).isSameAs(Connection.Status.CONNECTED);
        assertThat(connection.getConnectedUrl()).isEqualTo(expectedUrl);
    }

    private static String nextText(Subscription sub) throws InterruptedException {
        Message msg = sub.nextMessage(FLUSH_TIMEOUT);
        assertThat(msg).isNotNull();
        return new String(msg.getData(), UTF_8);
    }

    private static String payloadText(Object payload) {
        if (payload instanceof byte[] bytes) {
            return new String(bytes, UTF_8);
        }
        return payload.toString();
    }

    private static Headers headersWithTrace(String traceId) {
        return new Headers().put(TRACE_HEADER, traceId);
    }

    private static void assertDefaultListenersCanHandleCallbacks(Connection connection) {
        assertThatCode(() -> connection.getOptions().getConnectionListener()
                .connectionEvent(connection, ConnectionListener.Events.CONNECTED)).doesNotThrowAnyException();
        assertThatCode(() -> connection.getOptions().getErrorListener().slowConsumerDetected(connection, null))
                .doesNotThrowAnyException();
        assertThatCode(() -> connection.getOptions().getErrorListener()
                .exceptionOccurred(connection, new RuntimeException("boom"))).doesNotThrowAnyException();
        assertThatCode(() -> connection.getOptions().getErrorListener().errorOccurred(connection, "boom"))
                .doesNotThrowAnyException();
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

    private record BinderFixture(NatsChannelProvisioner provisioner, NatsChannelBinder binder) implements AutoCloseable {
        private Connection connection() {
            return this.binder.getConnection();
        }

        @Override
        public void close() throws InterruptedException {
            connection().close();
        }
    }
}
