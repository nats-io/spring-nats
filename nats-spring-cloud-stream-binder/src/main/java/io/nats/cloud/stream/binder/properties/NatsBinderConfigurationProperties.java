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

package io.nats.cloud.stream.binder.properties;

import io.nats.spring.boot.autoconfigure.NatsConnectionProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.lang.Nullable;

@ConfigurationProperties(prefix = "nats.spring.cloud.stream.binder")
public class NatsBinderConfigurationProperties extends NatsConnectionProperties {
    @Nullable
    private String[] headersToEmbed;

    public NatsBinderConfigurationProperties() {
    }

    /**
     * @return custom Spring header names to embed when embedded headers are used, or {@code null} to use Spring Cloud Stream defaults
     */
    @Nullable
    public String[] getHeadersToEmbed() {
        return this.headersToEmbed;
    }

    /**
     * @param headersToEmbed custom Spring header names to embed, or {@code null} to use Spring Cloud Stream defaults
     */
    public void setHeadersToEmbed(@Nullable String[] headersToEmbed) {
        this.headersToEmbed = headersToEmbed;
    }
}
