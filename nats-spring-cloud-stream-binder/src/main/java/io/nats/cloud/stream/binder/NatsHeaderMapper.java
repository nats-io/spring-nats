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

import io.nats.client.Message;
import io.nats.client.impl.Headers;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.stream.binder.BinderHeaders;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.messaging.MessageHeaders;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

class NatsHeaderMapper {
    private static final Log logger = LogFactory.getLog(NatsHeaderMapper.class);

    private static final Set<String> RESERVED_HEADERS = Set.of(
            MessageHeaders.ID.toLowerCase(Locale.ROOT),
            MessageHeaders.TIMESTAMP.toLowerCase(Locale.ROOT),
            MessageHeaders.REPLY_CHANNEL.toLowerCase(Locale.ROOT),
            MessageHeaders.ERROR_CHANNEL.toLowerCase(Locale.ROOT),
            BinderHeaders.NATIVE_HEADERS_PRESENT.toLowerCase(Locale.ROOT),
            IntegrationMessageHeaderAccessor.ACKNOWLEDGMENT_CALLBACK.toLowerCase(Locale.ROOT),
            NatsMessageProducer.SUBJECT.toLowerCase(Locale.ROOT)
    );

    private NatsHeaderMapper() {
    }

    static Headers fromSpringHeaders(MessageHeaders springHeaders) {
        Headers natsHeaders = new Headers();

        springHeaders.forEach((name, value) -> addHeader(natsHeaders, name, value));

        return natsHeaders.isEmpty() ? null : natsHeaders;
    }

    static Map<String, Object> toSpringHeaders(Message message, boolean includeNativeHeaders,
                                               boolean markNativeHeadersPresent) {
        Map<String, Object> springHeaders = new HashMap<>();

        if (message.hasHeaders()) {
            if (markNativeHeadersPresent) {
                springHeaders.put(BinderHeaders.NATIVE_HEADERS_PRESENT, true);
            }
            if (includeNativeHeaders) {
                message.getHeaders().forEach((name, values) -> {
                    if (!isReserved(name)) {
                        springHeaders.put(name, headerValue(values));
                    }
                });
            }
        }

        springHeaders.put(NatsMessageProducer.SUBJECT, message.getSubject());
        springHeaders.put(MessageHeaders.REPLY_CHANNEL, message.getReplyTo());
        return springHeaders;
    }

    private static void addHeader(Headers natsHeaders, String name, Object value) {
        if (!hasText(name) || isReserved(name) || value == null) {
            return;
        }

        if (value instanceof Collection<?>) {
            Collection<?> values = (Collection<?>) value;
            for (Object item : values) {
                if (item != null) {
                    addHeaderValue(natsHeaders, name, item.toString());
                }
            }
            return;
        }

        addHeaderValue(natsHeaders, name, value.toString());
    }

    private static void addHeaderValue(Headers natsHeaders, String name, String value) {
        try {
            natsHeaders.add(name, value);
        } catch (IllegalArgumentException exp) {
            if (logger.isDebugEnabled()) {
                logger.debug("Skipping Spring header '" + name + "' because it cannot be represented as a NATS protocol header");
            }
        }
    }

    private static Object headerValue(List<String> values) {
        if (values.size() == 1) {
            return values.get(0);
        }
        return new ArrayList<>(values);
    }

    private static boolean hasText(String text) {
        return text != null && !text.isBlank();
    }

    private static boolean isReserved(String name) {
        return name != null && RESERVED_HEADERS.contains(name.toLowerCase(Locale.ROOT));
    }
}
