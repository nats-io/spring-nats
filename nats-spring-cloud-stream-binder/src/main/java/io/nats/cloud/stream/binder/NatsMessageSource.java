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
import io.nats.client.FetchConsumeOptions;
import io.nats.client.FetchConsumer;
import io.nats.client.JetStreamApiException;
import io.nats.client.JetStreamStatusCheckedException;
import io.nats.client.Message;
import io.nats.client.Subscription;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.Lifecycle;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.acks.AcknowledgmentCallback;
import org.springframework.integration.endpoint.AbstractMessageSource;
import org.springframework.messaging.support.GenericMessage;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Message source for NATS connections, allowing synchronous polling.
 */
public class NatsMessageSource extends AbstractMessageSource<Object> implements Lifecycle {
    private static final Log logger = LogFactory.getLog(NatsMessageSource.class);

    private NatsConsumerDestination destination;
    private Connection connection;
    private Subscription sub;
    private AtomicReference<ConsumerContext> consumerContext = new AtomicReference<>();
    private AtomicReference<FetchConsumer> fetchConsumer = new AtomicReference<>();
    private boolean includeNativeHeaders;
    private boolean markNativeHeadersPresent;
    private boolean jetStream;
    private String streamName;
    private String consumerName;

    /**
     * Create a message source. Once started, the source will have a subscription but no threads.
     * Calls to doReceive result in a nextMessage call at the NATS level. Core NATS uses a
     * subscription, while JetStream uses a named ConsumerContext.
     *
     * @param destination where to subscribe
     * @param nc          NATS connection
     */
    public NatsMessageSource(NatsConsumerDestination destination, Connection nc) {
        this(destination, nc, true, true);
    }

    /**
     * Create a message source with explicit native header behavior.
     *
     * @param destination              where to subscribe
     * @param nc                       NATS connection
     * @param includeNativeHeaders     whether native NATS headers should be copied to Spring headers
     * @param markNativeHeadersPresent whether Spring Cloud Stream should be told native headers were present
     */
    public NatsMessageSource(NatsConsumerDestination destination, Connection nc,
                             boolean includeNativeHeaders, boolean markNativeHeadersPresent) {
        this(destination, nc, includeNativeHeaders, markNativeHeadersPresent, false, null, null);
    }

    /**
     * Create a message source with explicit native header and JetStream behavior.
     *
     * @param destination              where to subscribe
     * @param nc                       NATS connection
     * @param includeNativeHeaders     whether native NATS headers should be copied to Spring headers
     * @param markNativeHeadersPresent whether Spring Cloud Stream should be told native headers were present
     * @param jetStream                whether messages should be consumed through JetStream
     * @param streamName               optional JetStream stream name
     * @param consumerName             optional JetStream consumer name
     */
    public NatsMessageSource(NatsConsumerDestination destination, Connection nc,
                             boolean includeNativeHeaders, boolean markNativeHeadersPresent,
                             boolean jetStream, String streamName, String consumerName) {
        this.destination = destination;
        this.connection = nc;
        this.includeNativeHeaders = includeNativeHeaders;
        this.markNativeHeadersPresent = markNativeHeadersPresent;
        this.jetStream = jetStream;
        this.streamName = NatsJetStreamSupport.normalize(streamName);
        this.consumerName = NatsJetStreamSupport.normalize(consumerName);
    }

    @Override
    protected Object doReceive() {
        ConsumerContext context = this.consumerContext.get();
        if (!this.jetStream && this.sub == null) {
            return null;
        }
        if (this.jetStream && context == null) {
            return null;
        }

        try {
            Message m;
            if (this.jetStream) {
                m = receiveJetStreamMessage(context);
            } else {
                m = this.sub.nextMessage(Duration.ZERO);
            }

            if (this.jetStream && this.consumerContext.get() != context) {
                return null;
            }

            if (m != null && !m.isStatusMessage()) {
                Map<String, Object> headers = NatsHeaderMapper.toSpringHeaders(
                        m,
                        this.includeNativeHeaders,
                        this.markNativeHeadersPresent);
                if (this.jetStream) {
                    headers.put(IntegrationMessageHeaderAccessor.ACKNOWLEDGMENT_CALLBACK,
                            new JetStreamAcknowledgmentCallback(m));
                }
                return new GenericMessage<>(m.getData(), headers);
            }
        } catch (InterruptedException exp) {
            Thread.currentThread().interrupt();
            logger.info("wait for message interrupted");
        } catch (IOException | JetStreamApiException | JetStreamStatusCheckedException exp) {
            logger.warn("exception receiving JetStream message", exp);
        }

        return null;
    }

    @Override
    public boolean isRunning() {
        return this.jetStream ? this.consumerContext.get() != null : this.sub != null;
    }

    @Override
    public void start() {
        if (isRunning()) {
            return;
        }

        String sub = this.destination.getSubject();
        String queue = this.destination.getQueueGroup();

        if (this.jetStream) {
            startJetStream(sub, queue);
            return;
        }

        if (queue != null && queue.length() > 0) {
            this.sub = this.connection.subscribe(sub, queue);
        } else {
            this.sub = this.connection.subscribe(sub);
        }
    }

    private void startJetStream(String sub, String queue) {
        String consumer = NatsJetStreamSupport.hasText(this.consumerName)
                ? this.consumerName
                : NatsJetStreamSupport.normalize(queue);
        if (!NatsJetStreamSupport.hasText(this.streamName)) {
            throw new IllegalStateException("NATS JetStream polled consumers require stream-name");
        }
        if (!NatsJetStreamSupport.hasText(consumer)) {
            throw new IllegalStateException("NATS JetStream polled consumers require consumer-name or a consumer group");
        }

        try {
            this.consumerContext.set(this.connection.jetStream().getConsumerContext(this.streamName, consumer));
        } catch (IOException | JetStreamApiException | IllegalArgumentException exp) {
            throw new IllegalStateException("Failed to subscribe to NATS JetStream subject " + sub, exp);
        }
    }

    @Override
    public void stop() {
        if (this.jetStream) {
            this.consumerContext.set(null);
            closeFetchConsumer(this.fetchConsumer.getAndSet(null));
            return;
        }

        if (this.sub == null) {
            return;
        }

        this.sub.unsubscribe();
        this.sub = null;
    }

    @Override
    public String getComponentType() {
        return "nats:message-source";
    }

    private Message receiveJetStreamMessage(ConsumerContext context)
            throws IOException, JetStreamApiException, InterruptedException, JetStreamStatusCheckedException {
        FetchConsumer consumer = context.fetch(FetchConsumeOptions.builder()
                .maxMessages(1)
                .expiresIn(NatsJetStreamSupport.DEFAULT_JETSTREAM_POLL_TIMEOUT.toMillis())
                .build());
        this.fetchConsumer.set(consumer);
        try {
            if (this.consumerContext.get() != context) {
                return null;
            }
            return consumer.nextMessage();
        } finally {
            this.fetchConsumer.compareAndSet(consumer, null);
            closeFetchConsumer(consumer);
        }
    }

    private static void closeFetchConsumer(FetchConsumer consumer) {
        if (consumer == null) {
            return;
        }
        try {
            consumer.close();
        } catch (Exception exp) {
            logger.debug("exception closing JetStream fetch consumer", exp);
        }
    }

    private static class JetStreamAcknowledgmentCallback implements AcknowledgmentCallback {
        private final Message message;
        private final AtomicBoolean acknowledged = new AtomicBoolean(false);

        JetStreamAcknowledgmentCallback(Message message) {
            this.message = message;
        }

        @Override
        public void acknowledge(Status status) {
            if (!this.acknowledged.compareAndSet(false, true)) {
                return;
            }

            if (Status.ACCEPT.equals(status)) {
                this.message.ack();
            } else {
                this.message.nak();
            }
        }

        @Override
        public boolean isAcknowledged() {
            return this.acknowledged.get();
        }
    }
}
