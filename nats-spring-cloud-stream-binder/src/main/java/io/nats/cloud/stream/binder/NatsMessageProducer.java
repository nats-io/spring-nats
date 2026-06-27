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
import io.nats.client.Dispatcher;
import io.nats.client.JetStream;
import io.nats.client.JetStreamApiException;
import io.nats.client.Message;
import io.nats.client.PushSubscribeOptions;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.Lifecycle;
import org.springframework.integration.core.MessageProducer;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;

import java.io.IOException;
import java.util.Map;

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
    private Connection connection;
    private MessageChannel output;
    private Dispatcher dispatcher;
    private boolean includeNativeHeaders;
    private boolean markNativeHeadersPresent;
    private boolean jetStream;
    private String streamName;
    private String durableName;

    /**
     * Create a message producer. Once started the producer will use a dispatcher, and the associated thread, to
     * listen for and handle incoming messages.
     *
     * @param destination where to subscribe
     * @param nc          NATS connection
     */
    public NatsMessageProducer(NatsConsumerDestination destination, Connection nc) {
        this(destination, nc, true, true);
    }

    /**
     * Create a message producer with explicit native header behavior.
     *
     * @param destination              where to subscribe
     * @param nc                       NATS connection
     * @param includeNativeHeaders     whether native NATS headers should be copied to Spring headers
     * @param markNativeHeadersPresent whether Spring Cloud Stream should be told native headers were present
     */
    public NatsMessageProducer(NatsConsumerDestination destination, Connection nc,
                               boolean includeNativeHeaders, boolean markNativeHeadersPresent) {
        this(destination, nc, includeNativeHeaders, markNativeHeadersPresent, false, null, null);
    }

    /**
     * Create a message producer with explicit native header and JetStream behavior.
     *
     * @param destination              where to subscribe
     * @param nc                       NATS connection
     * @param includeNativeHeaders     whether native NATS headers should be copied to Spring headers
     * @param markNativeHeadersPresent whether Spring Cloud Stream should be told native headers were present
     * @param jetStream                whether messages should be consumed through JetStream
     * @param streamName               optional JetStream stream name
     * @param durableName              optional JetStream durable consumer name
     */
    public NatsMessageProducer(NatsConsumerDestination destination, Connection nc,
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
    public MessageChannel getOutputChannel() {
        return this.output;
    }

    @Override
    public void setOutputChannel(MessageChannel outputChannel) {
        this.output = outputChannel;
    }

    @Override
    public boolean isRunning() {
        return this.dispatcher != null;
    }

    @Override
    public void start() {
        if (this.dispatcher != null) {
            return;
        }

        if (this.jetStream) {
            startJetStream();
            return;
        }

        this.dispatcher = this.connection.createDispatcher(this::handleIncomingMessage);

        String sub = this.destination.getSubject();
        String queue = this.destination.getQueueGroup();

        if (queue != null && queue.length() > 0) {
            this.dispatcher.subscribe(sub, queue);
        } else {
            this.dispatcher.subscribe(sub);
        }
    }

    private void startJetStream() {
        this.dispatcher = this.connection.createDispatcher();

        String sub = this.destination.getSubject();
        String queue = this.destination.getQueueGroup();

        try {
            JetStream js = this.connection.jetStream();
            PushSubscribeOptions options = pushSubscribeOptions();
            if (queue != null && queue.length() > 0) {
                js.subscribe(sub, queue, this.dispatcher, this::handleIncomingMessage, false, options);
            } else {
                js.subscribe(sub, this.dispatcher, this::handleIncomingMessage, false, options);
            }
        } catch (IOException | JetStreamApiException | IllegalArgumentException exp) {
            this.connection.closeDispatcher(this.dispatcher);
            this.dispatcher = null;
            throw new IllegalStateException("Failed to subscribe to NATS JetStream subject " + sub, exp);
        }
    }

    private void handleIncomingMessage(Message msg) {
        if (this.output == null) {
            logger.warn("skipping message, no output channel set for " + this.destination.getName());
            return;
        }

        try {
            Map<String, Object> headers = NatsHeaderMapper.toSpringHeaders(
                    msg,
                    this.includeNativeHeaders,
                    this.markNativeHeadersPresent);
            GenericMessage<byte[]> m = new GenericMessage<byte[]>(msg.getData(), headers);
            if (this.output.send(m) && this.jetStream) {
                msg.ack();
            }
        } catch (Exception e) {
            logger.warn("exception sending message to output channel", e);
        }
    }

    @Override
    public void stop() {
        if (this.dispatcher == null) {
            return;
        }

        this.connection.closeDispatcher(this.dispatcher);
        this.dispatcher = null;
    }

    private PushSubscribeOptions pushSubscribeOptions() {
        PushSubscribeOptions.Builder builder = PushSubscribeOptions.builder();
        if (hasText(this.streamName)) {
            builder.stream(this.streamName);
        }
        if (hasText(this.durableName)) {
            builder.durable(this.durableName);
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
}
