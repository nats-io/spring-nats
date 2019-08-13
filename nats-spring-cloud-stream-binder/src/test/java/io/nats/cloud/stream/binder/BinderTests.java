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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import io.nats.spring.boot.autoconfigure.NatsAutoConfiguration;
import io.nats.spring.boot.autoconfigure.NatsProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import io.nats.cloud.stream.binder.properties.NatsBinderConfigurationProperties;
import io.nats.cloud.stream.binder.properties.NatsExtendedBindingProperties;
import org.springframework.cloud.stream.provisioning.ConsumerDestination;
import org.springframework.cloud.stream.provisioning.ProducerDestination;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.GenericMessage;

import io.nats.client.Connection;
import io.nats.client.Message;
import io.nats.client.Subscription;

public class BinderTests {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(NatsAutoConfiguration.class));

    @Test
    public void createBinderFromGlobalProperties() throws IOException, InterruptedException {
        try (NatsBinderTestServer ts = new NatsBinderTestServer()) {
            this.contextRunner.withPropertyValues().run((context) -> {
                NatsExtendedBindingProperties props = new NatsExtendedBindingProperties();
                NatsChannelBinderConfiguration config = new NatsChannelBinderConfiguration();
                NatsChannelProvisioner provisioner = config.natsChannelProvisioner();
                NatsBinderConfigurationProperties binderProps = new NatsBinderConfigurationProperties();
                config.setNatsProperties((NatsProperties) new NatsProperties().server(ts.getURI()));
                config.setNatsBinderConfigurationProperties(binderProps);
                config.setNatsExtendedBindingProperties(props);
                
                config.natsBinder(provisioner);
            });
        }
    }

    @Test
    public void createBinderFromBinderProperties() throws IOException, InterruptedException {
        try (NatsBinderTestServer ts = new NatsBinderTestServer()) {
            this.contextRunner.withPropertyValues().run((context) -> {
                NatsExtendedBindingProperties props = new NatsExtendedBindingProperties();
                NatsChannelBinderConfiguration config = new NatsChannelBinderConfiguration();
                NatsChannelProvisioner provisioner = config.natsChannelProvisioner();
                NatsBinderConfigurationProperties binderProps = new NatsBinderConfigurationProperties();
                config.setNatsProperties(new NatsProperties());
                config.setNatsBinderConfigurationProperties((NatsBinderConfigurationProperties) binderProps.server(ts.getURI()));
                config.setNatsExtendedBindingProperties(props);
                
                config.natsBinder(provisioner);
            });
        }
    }

    @Test
    public void createBinderWithoutServerProperties() throws IOException, InterruptedException {
        try (NatsBinderTestServer ts = new NatsBinderTestServer()) {
            this.contextRunner.withPropertyValues().run((context) -> {
                NatsExtendedBindingProperties props = new NatsExtendedBindingProperties();
                NatsChannelBinderConfiguration config = new NatsChannelBinderConfiguration();
                NatsChannelProvisioner provisioner = config.natsChannelProvisioner();
                NatsBinderConfigurationProperties binderProps = new NatsBinderConfigurationProperties();
                config.setNatsProperties(new NatsProperties());
                config.setNatsBinderConfigurationProperties(binderProps);
                config.setNatsExtendedBindingProperties(props);
                
                assertNull(config.natsBinder(provisioner));
            });
        }
    }

    @Test
    public void createTLSBinderFromGlobalProperties() throws IOException, InterruptedException {
        try (NatsBinderTestServer ts = new NatsBinderTestServer("src/test/resources/tls.conf", false)) {
            this.contextRunner.withPropertyValues("nats.spring.server=" + ts.getURI(),
                                                    "nats.spring.connectionTimeout=15s",
                                                    "nats.spring.keystorepath=src/test/resources/keystore.jks",
                                                    "nats.spring.keystorepassword=password",
                                                    "nats.spring.truststorepath=src/test/resources/cacerts",
                                                    "nats.spring.truststorepassword=password").run((context) -> {
                NatsExtendedBindingProperties props = new NatsExtendedBindingProperties();
                NatsChannelBinderConfiguration config = new NatsChannelBinderConfiguration();
                NatsChannelProvisioner provisioner = config.natsChannelProvisioner();
                NatsBinderConfigurationProperties binderProps = new NatsBinderConfigurationProperties();
                config.setNatsProperties((NatsProperties) new NatsProperties().server(ts.getURI()));
                config.setNatsBinderConfigurationProperties(binderProps);
                config.setNatsExtendedBindingProperties(props);
                
                config.natsBinder(provisioner);
            });
        }
    }
        
    @Test
    public void testMessageProducer() throws IOException, InterruptedException {
        try (NatsBinderTestServer ts = new NatsBinderTestServer()) {
            this.contextRunner.withPropertyValues("nats.spring.server:" + ts.getURI()).run((context) -> {
                Connection conn = (Connection) context.getBean(Connection.class);
                assertNotNull(conn);
                assertTrue("Connected Status", Connection.Status.CONNECTED == conn.getStatus());

                NatsExtendedBindingProperties props = new NatsExtendedBindingProperties();
                NatsChannelBinderConfiguration config = new NatsChannelBinderConfiguration();
                NatsChannelProvisioner provisioner = config.natsChannelProvisioner();
                NatsBinderConfigurationProperties binderProps = new NatsBinderConfigurationProperties();
                config.setNatsProperties((NatsProperties) new NatsProperties().server(ts.getURI()));
                config.setNatsBinderConfigurationProperties(binderProps);
                config.setNatsExtendedBindingProperties(props);
                NatsChannelBinder binder = config.natsBinder(provisioner);

                String theMessage = "hello world";
                String in = "in";

                ConsumerDestination from = provisioner.provisionConsumerDestination(in, "", null);
                NatsMessageProducer producer = (NatsMessageProducer) binder.createConsumerEndpoint(from, "", null);

                CompletableFuture<String> received = new CompletableFuture<>();
                DirectChannel output = new DirectChannel();
                output.subscribe(msg -> {
                    Object payload = msg.getPayload();

                    if (payload instanceof byte[]) {
                        received.complete(new String((byte[]) payload, StandardCharsets.UTF_8));
                    } else {
                        received.complete(payload.toString());
                    }
                });
                producer.setOutputChannel(output);

                assertTrue(producer.getOutputChannel() == output);

                assertFalse(producer.isRunning());
                producer.start();
                assertTrue(producer.isRunning());

                conn.publish(in, theMessage.getBytes(StandardCharsets.UTF_8));
                conn.flush(Duration.ofSeconds(5));

                String result = received.get(5, TimeUnit.SECONDS);

                assertEquals(theMessage, result);

                producer.stop();
            });
        }
    }

    @Test
    public void testMessageProducerWithGroup() throws IOException, InterruptedException {
        try (NatsBinderTestServer ts = new NatsBinderTestServer()) {
            this.contextRunner.withPropertyValues("nats.spring.server:" + ts.getURI()).run((context) -> {
                Connection conn = (Connection) context.getBean(Connection.class);
                assertNotNull(conn);
                assertTrue("Connected Status", Connection.Status.CONNECTED == conn.getStatus());

                NatsExtendedBindingProperties props = new NatsExtendedBindingProperties();
                NatsChannelBinderConfiguration config = new NatsChannelBinderConfiguration();
                NatsChannelProvisioner provisioner = config.natsChannelProvisioner();
                NatsBinderConfigurationProperties binderProps = new NatsBinderConfigurationProperties();
                config.setNatsProperties((NatsProperties) new NatsProperties().server(ts.getURI()));
                config.setNatsBinderConfigurationProperties(binderProps);
                config.setNatsExtendedBindingProperties(props);
                NatsChannelBinder binder = config.natsBinder(provisioner);

                String theMessage = "hello world";
                String in = "in";
                String group = "group";

                ConsumerDestination from = provisioner.provisionConsumerDestination(in, group, null);
                
                AtomicInteger counter = new AtomicInteger(0);
                
                NatsMessageProducer producer = (NatsMessageProducer) binder.createConsumerEndpoint(from, group, null);
                DirectChannel output = new DirectChannel();
                output.subscribe(msg -> {
                    counter.incrementAndGet();
                });
                producer.setOutputChannel(output);
                
                NatsMessageProducer producer2 = (NatsMessageProducer) binder.createConsumerEndpoint(from, group, null);
                DirectChannel output2 = new DirectChannel();
                output2.subscribe(msg -> {
                    counter.incrementAndGet();
                });
                producer2.setOutputChannel(output2);

                producer.start();
                producer2.start();

                int total = 100;

                for (int i=0; i<total; i++) {
                    conn.publish(in, theMessage.getBytes(StandardCharsets.UTF_8));
                }
                conn.flush(Duration.ofSeconds(5));
                
                // make sure the messages get through
                try {
                    Thread.sleep(2000);
                } catch (Exception exp) {

                }

                assertEquals(total, counter.get());

                producer.stop();
                producer2.stop();
            });
        }
    }

    @Test
    public void testMessageSource() throws IOException, InterruptedException {
        try (NatsBinderTestServer ts = new NatsBinderTestServer()) {
            this.contextRunner.withPropertyValues("nats.spring.server:" + ts.getURI()).run((context) -> {
                Connection conn = (Connection) context.getBean(Connection.class);
                assertNotNull(conn);
                assertTrue("Connected Status", Connection.Status.CONNECTED == conn.getStatus());

                NatsExtendedBindingProperties props = new NatsExtendedBindingProperties();
                NatsChannelBinderConfiguration config = new NatsChannelBinderConfiguration();
                NatsChannelProvisioner provisioner = config.natsChannelProvisioner();
                NatsBinderConfigurationProperties binderProps = new NatsBinderConfigurationProperties();
                config.setNatsProperties((NatsProperties) new NatsProperties().server(ts.getURI()));
                config.setNatsBinderConfigurationProperties(binderProps);
                config.setNatsExtendedBindingProperties(props);
                NatsChannelBinder binder = config.natsBinder(provisioner);

                String theMessage = "hello world";
                String in = "in";

                NatsConsumerDestination from = (NatsConsumerDestination) provisioner.provisionConsumerDestination(in, "", null);
                NatsMessageSource src = new NatsMessageSource(from, binder.getConnection());

                assertFalse(src.isRunning());
                src.start();
                assertTrue(src.isRunning());
                
                CompletableFuture<String> received = new CompletableFuture<>();

                Thread t = new Thread(() -> {
                    org.springframework.messaging.Message<Object> msg = src.receive();
                    Object payload = msg.getPayload();

                    if (payload instanceof byte[]) {
                        received.complete(new String((byte[]) payload, StandardCharsets.UTF_8));
                    } else {
                        received.complete(payload.toString());
                    }
                });
                t.start();

                conn.publish(in, theMessage.getBytes(StandardCharsets.UTF_8));
                conn.flush(Duration.ofSeconds(5));

                String result = received.get(5, TimeUnit.SECONDS);

                assertEquals(theMessage, result);

                src.stop();
            });
        }
    }

    @Test
    public void testMessageSourceWithQueue() throws IOException, InterruptedException {
        try (NatsBinderTestServer ts = new NatsBinderTestServer()) {
            this.contextRunner.withPropertyValues("nats.spring.server:" + ts.getURI()).run((context) -> {
                Connection conn = (Connection) context.getBean(Connection.class);
                assertNotNull(conn);
                assertTrue("Connected Status", Connection.Status.CONNECTED == conn.getStatus());

                NatsExtendedBindingProperties props = new NatsExtendedBindingProperties();
                NatsChannelBinderConfiguration config = new NatsChannelBinderConfiguration();
                NatsChannelProvisioner provisioner = config.natsChannelProvisioner();
                NatsBinderConfigurationProperties binderProps = new NatsBinderConfigurationProperties();
                config.setNatsProperties((NatsProperties) new NatsProperties().server(ts.getURI()));
                config.setNatsBinderConfigurationProperties(binderProps);
                config.setNatsExtendedBindingProperties(props);
                NatsChannelBinder binder = config.natsBinder(provisioner);

                String theMessage = "hello world";
                String in = "in";
                String group = "group";

                NatsConsumerDestination from = (NatsConsumerDestination) provisioner.provisionConsumerDestination(in, group, null);
                NatsMessageSource src = new NatsMessageSource(from, binder.getConnection());

                assertFalse(src.isRunning());
                src.start();
                assertTrue(src.isRunning());
                
                CompletableFuture<String> received = new CompletableFuture<>();

                Thread t = new Thread(() -> {
                    org.springframework.messaging.Message<Object> msg = src.receive();
                    Object payload = msg.getPayload();

                    if (payload instanceof byte[]) {
                        received.complete(new String((byte[]) payload, StandardCharsets.UTF_8));
                    } else {
                        received.complete(payload.toString());
                    }
                });
                t.start();

                conn.publish(in, theMessage.getBytes(StandardCharsets.UTF_8));
                conn.flush(Duration.ofSeconds(5));

                String result = received.get(5, TimeUnit.SECONDS);

                assertEquals(theMessage, result);

                src.stop();
            });
        }
    }

    @Test
    public void testMessageHandler() throws IOException, InterruptedException {
        try (NatsBinderTestServer ts = new NatsBinderTestServer()) {
            this.contextRunner.withPropertyValues("nats.spring.server:" + ts.getURI()).run((context) -> {
                Connection conn = (Connection) context.getBean(Connection.class);
                assertNotNull(conn);
                assertTrue("Connected Status", Connection.Status.CONNECTED == conn.getStatus());

                NatsExtendedBindingProperties props = new NatsExtendedBindingProperties();
                NatsChannelBinderConfiguration config = new NatsChannelBinderConfiguration();
                NatsChannelProvisioner provisioner = config.natsChannelProvisioner();
                NatsBinderConfigurationProperties binderProps = new NatsBinderConfigurationProperties();
                config.setNatsProperties((NatsProperties) new NatsProperties().server(ts.getURI()));
                config.setNatsBinderConfigurationProperties(binderProps);
                config.setNatsExtendedBindingProperties(props);
                NatsChannelBinder binder = config.natsBinder(provisioner);

                String theMessage = "hello world";
                String out = "out";
                ProducerDestination to = provisioner.provisionProducerDestination(out, null);
                MessageHandler mh = binder.createProducerMessageHandler(to, null, null);

                Subscription sub = conn.subscribe(out);
                conn.flush(Duration.ofSeconds(5));

                // send a byte array
                mh.handleMessage(new GenericMessage<byte[]>(theMessage.getBytes(StandardCharsets.UTF_8)));
                Message msg = sub.nextMessage(Duration.ofSeconds(5));
                String result = (msg != null) ? new String((byte[]) msg.getData(), StandardCharsets.UTF_8) : null;
                assertEquals(theMessage, result);

                // send a byte buffer
                ByteBuffer buffer = ByteBuffer.wrap(theMessage.getBytes(StandardCharsets.UTF_8));
                mh.handleMessage(new GenericMessage<ByteBuffer>(buffer));
                msg = sub.nextMessage(Duration.ofSeconds(5));
                result = (msg != null) ? new String((byte[]) msg.getData(), StandardCharsets.UTF_8) : null;
                assertEquals(theMessage, result);

                // send a string
                mh.handleMessage(new GenericMessage<String>(theMessage));
                msg = sub.nextMessage(Duration.ofSeconds(5));
                result = (msg != null) ? new String((byte[]) msg.getData(), StandardCharsets.UTF_8) : null;
                assertEquals(theMessage, result);

                // send an unknown type
                mh.handleMessage(new GenericMessage<Integer>(new Integer(2)));
                msg = sub.nextMessage(Duration.ofSeconds(5));
                assertNull(msg);
            });
        }
    }
}