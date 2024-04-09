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

package io.nats.spring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class MultiConnectTransformer {
    private static final Logger LOG = LoggerFactory.getLogger(MultiConnectTransformer.class);

    private static Object getMessage(String value) {
        String reverse = IntStream.iterate(value.length() - 1, i -> i >= 0, i -> i - 1)
                .mapToObj(i -> String.valueOf(value.charAt(i))).collect(Collectors.joining());
        return reverse.getBytes(StandardCharsets.UTF_8);
    }

    @Bean
    public Function<Object, Object> transform() {
        return message -> {
            String messageValue = null;
            if (message instanceof byte[]) {
                messageValue = new String((byte[]) message, StandardCharsets.UTF_8);
            } else if (message instanceof String value) {
                messageValue = value;
            }
            LOG.info("Message : {}", messageValue);
            return Objects.isNull(messageValue) ? null : getMessage(messageValue);
        };
    }
}
