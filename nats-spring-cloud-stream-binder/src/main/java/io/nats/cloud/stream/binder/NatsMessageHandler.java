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
import io.nats.client.PublishOptions;
import io.nats.client.impl.Headers;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessageHeaders;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * NatsMessageHandlers implement the standard message handling pattern. byte[], ByteBuffer and Strings are supported, where
 * Strings are treated as UTF8 bytes.
 */
public class NatsMessageHandler extends AbstractMessageHandler {
    private static final Log logger = LogFactory.getLog(NatsMessageHandler.class);

    private String subject;
    @Nullable
    private Connection connection;
    private boolean publishHeaders;
    private boolean jetStream;
    @Nullable
    private String streamName;
    @Nullable
    private JetStream jetStreamContext;

    /**
     * Create a handler with a specific, unchanging subject, and a NATS connection.
     *
     * @param subject subject to publish messages to by default; must not be {@code null}
     * @param nc      NATS connection, or {@code null} when connection setup failed
     * @throws NullPointerException if {@code subject} is {@code null}
     */
    public NatsMessageHandler(String subject, @Nullable Connection nc) {
        this(subject, nc, true);
    }

    /**
     * Create a handler with a specific, unchanging subject, a NATS connection, and header mode.
     *
     * @param subject        subject to publish messages to by default; must not be {@code null}
     * @param nc             NATS connection, or {@code null} when connection setup failed
     * @param publishHeaders whether Spring headers should be published as native NATS headers
     * @throws NullPointerException if {@code subject} is {@code null}
     */
    public NatsMessageHandler(String subject, @Nullable Connection nc, boolean publishHeaders) {
        this(subject, nc, publishHeaders, false, null);
    }

    /**
     * Create a handler with a specific, unchanging subject, a NATS connection, header mode, and JetStream mode.
     *
     * @param subject        subject to publish messages to by default; must not be {@code null}
     * @param nc             NATS connection, or {@code null} when connection setup failed
     * @param publishHeaders whether Spring headers should be published as native NATS headers
     * @param jetStream      whether messages should be published through JetStream
     * @param streamName     optional JetStream stream name; {@code null} lets the client resolve the target stream
     * @throws NullPointerException if {@code subject} is {@code null}
     */
    public NatsMessageHandler(String subject, @Nullable Connection nc, boolean publishHeaders, boolean jetStream,
                              @Nullable String streamName) {
        this.subject = Objects.requireNonNull(subject, "subject must not be null");
        this.connection = nc;
        this.publishHeaders = publishHeaders;
        this.jetStream = jetStream;
        this.streamName = NatsJetStreamSupport.normalize(streamName);
        this.jetStreamContext = jetStreamContext(nc, jetStream);
    }

    /**
     * Given a message, take the payload and publish it on the handlers subject.
     * If the message contains a MessageHeaders.REPLY_CHANNEL header, it is passed on to allow taking advantage of built-in request/reply handling
     * in Spring Integration / Spring Cloud Stream.
     */
    @Override
    protected void handleMessageInternal(Message<?> message) {
        Object payload = message.getPayload();
        byte[] bytes = null;

        if (payload instanceof byte[]) {
            bytes = (byte[]) payload;
        } else if (payload instanceof ByteBuffer) {
            ByteBuffer buf = ((ByteBuffer) payload);
            bytes = new byte[buf.remaining()];
            buf.get(bytes);
        } else if (payload instanceof String) {
            bytes = ((String) payload).getBytes(StandardCharsets.UTF_8);
        }

        if (bytes == null) {
            logger.warn("NATS handler only supports byte array, byte buffer and string messages");
            return;
        }

        Connection nc = this.connection;
        if (nc != null) {
            final Object replyChannel = message.getHeaders().get(MessageHeaders.REPLY_CHANNEL);
            final String replyTo = replyChannel != null ? replyChannel.toString() : null;
            Headers headers = this.publishHeaders ? NatsHeaderMapper.fromSpringHeaders(message.getHeaders()) : null;
            publishMessage(nc, message, bytes, replyTo, headers);
        }
    }

    private void publishMessage(Connection nc, Message<?> message, byte[] bytes, @Nullable String replyTo,
                                @Nullable Headers headers) {
        if (this.jetStream) {
            publishJetStreamMessage(message, bytes, replyTo, headers);
            return;
        }

        if (headers == null) {
            nc.publish(this.subject, replyTo, bytes);
        } else {
            nc.publish(this.subject, replyTo, headers, bytes);
        }
    }

    private void publishJetStreamMessage(Message<?> message, byte[] bytes, @Nullable String replyTo,
                                         @Nullable Headers headers) {
        if (replyTo != null) {
            throw new MessageHandlingException(message, "JetStream publishing does not support reply channels");
        }

        JetStream js = this.jetStreamContext;
        if (js == null) {
            throw new MessageHandlingException(message, "JetStream context is not available");
        }

        try {
            PublishOptions publishOptions = publishOptions();
            if (headers == null) {
                if (publishOptions == null) {
                    js.publish(this.subject, bytes);
                } else {
                    js.publish(this.subject, bytes, publishOptions);
                }
            } else if (publishOptions == null) {
                js.publish(this.subject, headers, bytes);
            } else {
                js.publish(this.subject, headers, bytes, publishOptions);
            }
        } catch (IOException | JetStreamApiException | IllegalArgumentException exp) {
            throw new MessageHandlingException(message,
                    "Failed to publish message to NATS JetStream subject " + this.subject, exp);
        }
    }

    @Nullable
    private PublishOptions publishOptions() {
        if (!NatsJetStreamSupport.hasText(this.streamName)) {
            return null;
        }

        return PublishOptions.builder()
                .stream(this.streamName)
                .build();
    }

    @Nullable
    private static JetStream jetStreamContext(@Nullable Connection nc, boolean jetStream) {
        if (!jetStream || nc == null) {
            return null;
        }

        try {
            return nc.jetStream();
        } catch (IOException exp) {
            throw new IllegalStateException("Failed to create NATS JetStream context", exp);
        }
    }
}
