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
import org.springframework.lang.Nullable;

public class NatsProducerProperties {
    private boolean jetStream;
    @Nullable
    private String streamName;
    private boolean provisionStream;
    @Nullable
    private StorageType streamStorageType;
    @Nullable
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
     * @return JetStream stream name used for publish acknowledgements, or {@code null} when no stream name was configured
     */
    @Nullable
    public String getStreamName() {
        return this.streamName;
    }

    /**
     * @param streamName JetStream stream name used for publish acknowledgements, or {@code null} to leave it unset
     */
    public void setStreamName(@Nullable String streamName) {
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
     * @return storage type used when a missing JetStream stream is provisioned, or {@code null} to use the NATS client default
     */
    @Nullable
    public StorageType getStreamStorageType() {
        return this.streamStorageType;
    }

    /**
     * @param streamStorageType storage type used when a missing JetStream stream is provisioned, or {@code null} to use the NATS client default
     */
    public void setStreamStorageType(@Nullable StorageType streamStorageType) {
        this.streamStorageType = streamStorageType;
    }

    /**
     * @return replica count used when a missing JetStream stream is provisioned, or {@code null} to use the NATS client default
     */
    @Nullable
    public Integer getStreamReplicas() {
        return this.streamReplicas;
    }

    /**
     * @param streamReplicas replica count used when a missing JetStream stream is provisioned, or {@code null} to use the NATS client default
     */
    public void setStreamReplicas(@Nullable Integer streamReplicas) {
        this.streamReplicas = streamReplicas;
    }
}
