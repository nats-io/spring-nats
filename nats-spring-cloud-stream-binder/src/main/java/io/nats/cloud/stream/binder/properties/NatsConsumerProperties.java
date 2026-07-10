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

import org.springframework.lang.Nullable;

public class NatsConsumerProperties {
    private boolean jetStream;
    @Nullable
    private String streamName;
    @Nullable
    private String consumerName;

    /**
     * @return whether this consumer subscribes through JetStream instead of core NATS
     */
    public boolean isJetStream() {
        return this.jetStream;
    }

    /**
     * @param jetStream whether this consumer subscribes through JetStream instead of core NATS
     */
    public void setJetStream(boolean jetStream) {
        this.jetStream = jetStream;
    }

    /**
     * @return JetStream stream name used for subscriptions, or {@code null} when no stream name was configured
     */
    @Nullable
    public String getStreamName() {
        return this.streamName;
    }

    /**
     * @param streamName JetStream stream name used for subscriptions, or {@code null} to leave it unset
     */
    public void setStreamName(@Nullable String streamName) {
        this.streamName = streamName;
    }

    /**
     * @return JetStream consumer name used for subscriptions, or {@code null} when no consumer name was configured
     */
    @Nullable
    public String getConsumerName() {
        return this.consumerName;
    }

    /**
     * @param consumerName JetStream consumer name used for subscriptions, or {@code null} to leave it unset
     */
    public void setConsumerName(@Nullable String consumerName) {
        this.consumerName = consumerName;
    }

}
