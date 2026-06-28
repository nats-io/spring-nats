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

import io.nats.client.api.StorageType;

public class NatsProducerProperties {
    private boolean jetStream;
    private String streamName;
    private boolean provisionStream;
    private StorageType streamStorageType;
    private Integer streamReplicas;

    /**
     * @return whether this producer publishes through JetStream instead of core NATS
     */
    public boolean isJetStream() {
        return this.jetStream;
    }

    /**
     * @param jetStream whether this producer publishes through JetStream instead of core NATS
     */
    public void setJetStream(boolean jetStream) {
        this.jetStream = jetStream;
    }

    /**
     * @return optional JetStream stream name used for publish acknowledgements
     */
    public String getStreamName() {
        return this.streamName;
    }

    /**
     * @param streamName optional JetStream stream name used for publish acknowledgements
     */
    public void setStreamName(String streamName) {
        this.streamName = streamName;
    }

    /**
     * @return whether the binder should create the configured JetStream stream when missing and validate subject coverage when it already exists
     */
    public boolean isProvisionStream() {
        return this.provisionStream;
    }

    /**
     * @param provisionStream whether the binder should create the configured JetStream stream when missing and validate subject coverage when it already exists
     */
    public void setProvisionStream(boolean provisionStream) {
        this.provisionStream = provisionStream;
    }

    /**
     * @return optional storage type used when a missing JetStream stream is provisioned
     */
    public StorageType getStreamStorageType() {
        return this.streamStorageType;
    }

    /**
     * @param streamStorageType optional storage type used when a missing JetStream stream is provisioned
     */
    public void setStreamStorageType(StorageType streamStorageType) {
        this.streamStorageType = streamStorageType;
    }

    /**
     * @return optional replica count used when a missing JetStream stream is provisioned
     */
    public Integer getStreamReplicas() {
        return this.streamReplicas;
    }

    /**
     * @param streamReplicas optional replica count used when a missing JetStream stream is provisioned
     */
    public void setStreamReplicas(Integer streamReplicas) {
        this.streamReplicas = streamReplicas;
    }
}
