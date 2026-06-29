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

public class NatsConsumerProperties {
    private boolean jetStream;
    private String streamName;
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
     * @return optional JetStream stream name used for subscriptions
     */
    public String getStreamName() {
        return this.streamName;
    }

    /**
     * @param streamName optional JetStream stream name used for subscriptions
     */
    public void setStreamName(String streamName) {
        this.streamName = streamName;
    }

    /**
     * @return optional JetStream consumer name used for subscriptions
     */
    public String getConsumerName() {
        return this.consumerName;
    }

    /**
     * @param consumerName optional JetStream consumer name used for subscriptions
     */
    public void setConsumerName(String consumerName) {
        this.consumerName = consumerName;
    }

}
