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

import io.nats.client.Connection;
import io.nats.client.ConsumerContext;
import io.nats.client.Dispatcher;
import io.nats.client.JetStreamApiException;
import io.nats.client.Message;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.Lifecycle;
import org.springframework.integration.core.MessageProducer;
import org.springframework.lang.Nullable;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * MessageProducer for NATS connections.
 */
public class NatsMessageProducer implements MessageProducer, Lifecycle {
    private static final Log logger = LogFactory.getLog(NatsMessageProducer.class);

    /**
     * The NATS subject for incoming message is stored in the SUBJECT header.
     */
    public static final String SUBJECT = "subject";

    private NatsConsumerDestination destination;
    @Nullable
    private Connection connection;
    @Nullable
    private MessageChannel output;
    private AtomicReference<Dispatcher> dispatcher = new AtomicReference<>();
    private AtomicReference<io.nats.client.MessageConsumer> jetStreamConsumer = new AtomicReference<>();
    private boolean includeNativeHeaders;
    private boolean markNativeHeadersPresent;
    private boolean jetStream;
    @Nullable
    private String streamName;
    @Nullable
    private String consumerName;

    /**
     * Create a message producer. Once started the producer will use a dispatcher, and the associated thread, to
     * listen for and handle incoming messages.
     *
     * @param destination destination to subscribe to; must not be {@code null}
     * @param nc          NATS connection, or {@code null} when connection setup failed
     * @throws NullPointerException if {@code destination} is {@code null}
     */
    public NatsMessageProducer(NatsConsumerDestination destination, @Nullable Connection nc) {
        this(destination, nc, true, true);
    }

    /**
     * Create a message producer with explicit native header behavior.
     *
     * @param destination              destination to subscribe to; must not be {@code null}
     * @param nc                       NATS connection, or {@code null} when connection setup failed
     * @param includeNativeHeaders     whether native NATS headers should be copied to Spring headers
     * @param markNativeHeadersPresent whether Spring Cloud Stream should be told native headers were present
     * @throws NullPointerException if {@code destination} is {@code null}
     */
    public NatsMessageProducer(NatsConsumerDestination destination, @Nullable Connection nc,
                               boolean includeNativeHeaders, boolean markNativeHeadersPresent) {
        this(destination, nc, includeNativeHeaders, markNativeHeadersPresent, false, null, null);
    }

    /**
     * Create a message producer with explicit native header and JetStream behavior.
     *
     * @param destination              destination to subscribe to; must not be {@code null}
     * @param nc                       NATS connection, or {@code null} when connection setup failed
     * @param includeNativeHeaders     whether native NATS headers should be copied to Spring headers
     * @param markNativeHeadersPresent whether Spring Cloud Stream should be told native headers were present
     * @param jetStream                whether messages should be consumed through JetStream
     * @param streamName               optional JetStream stream name; required before start when JetStream mode is enabled
     * @param consumerName             optional JetStream consumer name; required before start when JetStream mode is enabled and no consumer group exists
     * @throws NullPointerException if {@code destination} is {@code null}
     */
    public NatsMessageProducer(NatsConsumerDestination destination, @Nullable Connection nc,
                               boolean includeNativeHeaders, boolean markNativeHeadersPresent,
                               boolean jetStream, @Nullable String streamName, @Nullable String consumerName) {
        this.destination = Objects.requireNonNull(destination, "destination must not be null");
        this.connection = nc;
        this.includeNativeHeaders = includeNativeHeaders;
        this.markNativeHeadersPresent = markNativeHeadersPresent;
        this.jetStream = jetStream;
        this.streamName = NatsJetStreamSupport.normalize(streamName);
        this.consumerName = NatsJetStreamSupport.normalize(consumerName);
    }

    /**
     * @return output channel receiving Spring messages, or {@code null} when none has been configured
     */
    @Override
    @Nullable
    public MessageChannel getOutputChannel() {
        return this.output;
    }

    @Override
    public void setOutputChannel(MessageChannel outputChannel) {
        this.output = outputChannel;
    }

    @Override
    public boolean isRunning() {
        return this.dispatcher.get() != null;
    }

    @Override
    public void start() {
        if (this.dispatcher.get() != null) {
            return;
        }

        Connection nc = this.connection;
        if (nc == null) {
            logger.warn("cannot start NATS message producer, no connection available for "
                    + this.destination.getName());
            return;
        }

        if (this.jetStream) {
            startJetStream(nc);
            return;
        }

        Dispatcher dispatcher = nc.createDispatcher(this::handleIncomingMessage);
        this.dispatcher.set(dispatcher);

        String sub = this.destination.getSubject();
        String queue = this.destination.getQueueGroup();

        if (queue != null && queue.length() > 0) {
            dispatcher.subscribe(sub, queue);
        } else {
            dispatcher.subscribe(sub);
        }
    }

    private void startJetStream(Connection nc) {
        String sub = this.destination.getSubject();
        String queue = this.destination.getQueueGroup();
        String consumer = NatsJetStreamSupport.hasText(this.consumerName)
                ? this.consumerName
                : NatsJetStreamSupport.normalize(queue);

        if (!NatsJetStreamSupport.hasText(this.streamName)) {
            throw new IllegalStateException("NATS JetStream consumers require stream-name");
        }
        if (!NatsJetStreamSupport.hasText(consumer)) {
            throw new IllegalStateException("NATS JetStream consumers require consumer-name or a consumer group");
        }

        Dispatcher dispatcher = nc.createDispatcher();
        this.dispatcher.set(dispatcher);

        try {
            ConsumerContext consumerContext = nc.jetStream()
                    .getConsumerContext(this.streamName, consumer);
            this.jetStreamConsumer.set(consumerContext.consume(dispatcher, this::handleIncomingMessage));
        } catch (IOException | JetStreamApiException | IllegalArgumentException exp) {
            nc.closeDispatcher(dispatcher);
            this.dispatcher.compareAndSet(dispatcher, null);
            throw new IllegalStateException("Failed to subscribe to NATS JetStream subject " + sub, exp);
        }
    }

    private void handleIncomingMessage(Message msg) {
        if (this.output == null) {
            logger.warn("skipping message, no output channel set for " + this.destination.getName());
            if (this.jetStream) {
                msg.nak();
            }
            return;
        }

        try {
            Map<String, Object> headers = NatsHeaderMapper.toSpringHeaders(
                    msg,
                    this.includeNativeHeaders,
                    this.markNativeHeadersPresent);
            GenericMessage<byte[]> m = new GenericMessage<>(msg.getData(), headers);
            if (this.output.send(m)) {
                if (this.jetStream) {
                    msg.ack();
                }
            } else if (this.jetStream) {
                msg.nak();
            }
        } catch (Exception e) {
            logger.warn("exception sending message to output channel", e);
            if (this.jetStream) {
                msg.nak();
            }
        }
    }

    @Override
    public void stop() {
        Dispatcher dispatcher = this.dispatcher.getAndSet(null);
        if (dispatcher == null) {
            return;
        }

        io.nats.client.MessageConsumer consumer = this.jetStreamConsumer.getAndSet(null);
        if (consumer != null) {
            consumer.stop();
        }
        Connection nc = this.connection;
        if (nc != null) {
            nc.closeDispatcher(dispatcher);
        }
    }
}
