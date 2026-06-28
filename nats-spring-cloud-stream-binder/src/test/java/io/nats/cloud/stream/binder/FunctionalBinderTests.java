/*
 * Copyright 2017-2026 the original author or authors.
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

import io.nats.client.Connection;
import io.nats.client.Message;
import io.nats.client.Nats;
import io.nats.client.Subscription;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.support.MessageBuilder;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@ResourceLock(Resources.SYSTEM_PROPERTIES)
class FunctionalBinderTests {
    private static final Duration FLUSH_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration RECEIVE_TIMEOUT = Duration.ofSeconds(5);

    @Test
    void functionalConsumerReceivesNatsMessagesForIssue29() throws Exception {
        try (NatsBinderTestServer server = new NatsBinderTestServer()) {
            String subject = uniqueSubject("functional.consumer.issue29");
            try (ConfigurableApplicationContext context = runApplication(
                    FunctionalConsumerApplication.class,
                    server.getURI(),
                    "spring.cloud.function.definition=input",
                    "spring.cloud.stream.bindings.input-in-0.destination=" + subject,
                    "spring.cloud.stream.bindings.input-in-0.binder=nats");
                 Connection connection = Nats.connect(server.getURI())) {

                BlockingQueue<String> receivedMessages = context.getBean("receivedMessages", BlockingQueue.class);

                publishUntilReceived(connection, subject, "functional consumer", receivedMessages);
            }
        }
    }

    @Test
    void functionalFunctionTransformsNatsMessagesForIssue29() throws Exception {
        try (NatsBinderTestServer server = new NatsBinderTestServer()) {
            String inputSubject = uniqueSubject("functional.function.input.issue29");
            String outputSubject = uniqueSubject("functional.function.output.issue29");
            try (ConfigurableApplicationContext ignored = runApplication(
                    FunctionalFunctionApplication.class,
                    server.getURI(),
                    "spring.cloud.function.definition=transform",
                    "spring.cloud.stream.bindings.transform-in-0.destination=" + inputSubject,
                    "spring.cloud.stream.bindings.transform-in-0.binder=nats",
                    "spring.cloud.stream.bindings.transform-out-0.destination=" + outputSubject,
                    "spring.cloud.stream.bindings.transform-out-0.binder=nats");
                 Connection connection = Nats.connect(server.getURI())) {

                Subscription output = connection.subscribe(outputSubject);
                connection.flush(FLUSH_TIMEOUT);

                publishUntilOutput(connection, inputSubject, output, "functional function", "FUNCTIONAL FUNCTION");
            }
        }
    }

    @Test
    void streamBridgePublishesNatsMessagesForIssue29() throws Exception {
        try (NatsBinderTestServer server = new NatsBinderTestServer()) {
            String subject = uniqueSubject("functional.streambridge.issue29");
            try (ConfigurableApplicationContext context = runApplication(
                    StreamBridgeApplication.class,
                    server.getURI(),
                    "spring.cloud.stream.bindings.bridgeOut.destination=" + subject,
                    "spring.cloud.stream.bindings.bridgeOut.binder=nats");
                 Connection connection = Nats.connect(server.getURI())) {

                Subscription output = connection.subscribe(subject);
                connection.flush(FLUSH_TIMEOUT);
                StreamBridge streamBridge = context.getBean(StreamBridge.class);

                assertThat(streamBridge.send("bridgeOut",
                        MessageBuilder.withPayload("functional stream bridge".getBytes(UTF_8)).build())).isTrue();

                assertThat(nextText(output)).isEqualTo("functional stream bridge");
            }
        }
    }

    private static ConfigurableApplicationContext runApplication(Class<?> source, String server, String... properties) {
        String[] arguments = new String[properties.length + 2];
        arguments[0] = "--spring.jmx.enabled=false";
        arguments[1] = "--nats.spring.server=" + server;
        for (int index = 0; index < properties.length; index++) {
            arguments[index + 2] = "--" + properties[index];
        }

        return new SpringApplicationBuilder(source)
                .web(WebApplicationType.NONE)
                .run(arguments);
    }

    private static void publishUntilReceived(Connection connection,
                                             String subject,
                                             String payload,
                                             BlockingQueue<String> receivedMessages) throws Exception {
        long deadline = System.nanoTime() + RECEIVE_TIMEOUT.toNanos();
        List<String> actualMessages = new ArrayList<>();
        while (System.nanoTime() < deadline) {
            connection.publish(subject, payload.getBytes(UTF_8));
            connection.flush(FLUSH_TIMEOUT);
            String received = receivedMessages.poll(200, TimeUnit.MILLISECONDS);
            if (payload.equals(received)) {
                return;
            }
            if (received != null) {
                actualMessages.add(received);
            }
        }

        fail("Expected NATS subject <%s> to deliver <%s>, but received <%s>",
                subject,
                payload,
                actualMessages);
    }

    private static void publishUntilOutput(Connection connection,
                                           String subject,
                                           Subscription output,
                                           String payload,
                                           String expected) throws Exception {
        long deadline = System.nanoTime() + RECEIVE_TIMEOUT.toNanos();
        List<String> actualMessages = new ArrayList<>();
        while (System.nanoTime() < deadline) {
            connection.publish(subject, payload.getBytes(UTF_8));
            connection.flush(FLUSH_TIMEOUT);
            Message message = output.nextMessage(Duration.ofMillis(200));
            if (message != null) {
                String received = new String(message.getData(), UTF_8);
                if (expected.equals(received)) {
                    return;
                }
                actualMessages.add(received);
            }
        }

        fail("Expected NATS subject <%s> to emit transformed payload <%s> after publishing <%s>, but received <%s>",
                subject,
                expected,
                payload,
                actualMessages);
    }

    private static String nextText(Subscription subscription) throws InterruptedException {
        Message message = subscription.nextMessage(RECEIVE_TIMEOUT);
        assertThat(message).isNotNull();
        return new String(message.getData(), UTF_8);
    }

    private static String uniqueSubject(String prefix) {
        return prefix + "." + System.nanoTime();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class FunctionalConsumerApplication {
        @Bean
        BlockingQueue<String> receivedMessages() {
            return new LinkedBlockingQueue<>();
        }

        @Bean
        Consumer<byte[]> input(BlockingQueue<String> receivedMessages) {
            return payload -> receivedMessages.add(new String(payload, UTF_8));
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class FunctionalFunctionApplication {
        @Bean
        Function<byte[], byte[]> transform() {
            return payload -> new String(payload, UTF_8)
                    .toUpperCase(Locale.ROOT)
                    .getBytes(UTF_8);
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class StreamBridgeApplication {
    }
}
