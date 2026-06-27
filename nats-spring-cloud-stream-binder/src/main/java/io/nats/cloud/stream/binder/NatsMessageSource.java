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
import io.nats.client.JetStream;
import io.nats.client.JetStreamApiException;
import io.nats.client.JetStreamSubscription;
import io.nats.client.Message;
import io.nats.client.PullSubscribeOptions;
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

/**
 * Message source for NATS connections, allowing synchronous polling.
 */
public class NatsMessageSource extends AbstractMessageSource<Object> implements Lifecycle {
    private static final Log logger = LogFactory.getLog(NatsMessageSource.class);

    private NatsConsumerDestination destination;
    private Connection connection;
    private Subscription sub;
    private boolean includeNativeHeaders;
    private boolean markNativeHeadersPresent;
    private boolean jetStream;
    private String streamName;
    private String durableName;

    /**
     * Create a message source. Once started, the source will have a subscription but no threads.
     * Calls to doReceive result in a nextMessage call at the NATS level. Currently nextMessage is
     * called with Duration.ZERO and will wait forever.
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
     * @param durableName              optional JetStream durable consumer name
     */
    public NatsMessageSource(NatsConsumerDestination destination, Connection nc,
                             boolean includeNativeHeaders, boolean markNativeHeadersPresent,
                             boolean jetStream, String streamName, String durableName) {
        this.destination = destination;
        this.connection = nc;
        this.includeNativeHeaders = includeNativeHeaders;
        this.markNativeHeadersPresent = markNativeHeadersPresent;
        this.jetStream = jetStream;
        this.streamName = normalize(streamName);
        this.durableName = normalize(durableName);
    }

    @Override
    protected Object doReceive() {
        if (this.sub == null) {
            return null;
        }

        try {
            Message m;
            if (this.jetStream) {
                JetStreamSubscription jetStreamSub = (JetStreamSubscription) this.sub;
                jetStreamSub.pull(1);
                m = jetStreamSub.nextMessage(Duration.ZERO);
            } else {
                m = this.sub.nextMessage(Duration.ZERO);
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
                return new GenericMessage<byte[]>(m.getData(), headers);
            }
        } catch (InterruptedException exp) {
            logger.info("wait for message interrupted");
        }

        return null;
    }

    @Override
    public boolean isRunning() {
        return this.sub != null;
    }

    @Override
    public void start() {
        if (this.sub != null) {
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
        try {
            JetStream js = this.connection.jetStream();
            this.sub = js.subscribe(sub, pullSubscribeOptions(queue));
        } catch (IOException | JetStreamApiException | IllegalArgumentException exp) {
            throw new IllegalStateException("Failed to subscribe to NATS JetStream subject " + sub, exp);
        }
    }

    @Override
    public void stop() {
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

    private PullSubscribeOptions pullSubscribeOptions(String queue) {
        PullSubscribeOptions.Builder builder = PullSubscribeOptions.builder();
        if (hasText(this.streamName)) {
            builder.stream(this.streamName);
        }
        String durable = hasText(this.durableName) ? this.durableName : normalize(queue);
        if (hasText(durable)) {
            builder.durable(durable);
        }
        return builder.build();
    }

    private static String normalize(String value) {
        if (!hasText(value)) {
            return null;
        }

        return value.trim();
    }

    private static boolean hasText(String value) {
        return value != null && value.trim().length() > 0;
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
            } else if (Status.REQUEUE.equals(status)) {
                this.message.nak();
            } else {
                this.message.term();
            }
        }

        @Override
        public boolean isAcknowledged() {
            return this.acknowledged.get();
        }
    }
}
