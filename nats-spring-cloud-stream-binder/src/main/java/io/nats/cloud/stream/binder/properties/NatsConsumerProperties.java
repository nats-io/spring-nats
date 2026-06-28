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

import io.nats.client.api.DeliverPolicy;
import io.nats.client.api.ReplayPolicy;
import io.nats.client.api.StorageType;

import java.time.Duration;

public class NatsConsumerProperties {
    private boolean jetStream;
    private String streamName;
    private String durableName;
    private boolean provisionStream;
    private StorageType streamStorageType;
    private Integer streamReplicas;
    private Duration ackWait;
    private Long maxDeliver;
    private Long maxAckPending;
    private DeliverPolicy deliverPolicy;
    private ReplayPolicy replayPolicy;
    private Duration idleHeartbeat;
    private Boolean flowControl;
    private Duration inactiveThreshold;
    private Long maxPullWaiting;
    private Long maxBatch;
    private Long maxBytes;
    private Boolean ordered;
    private Duration pollTimeout;

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
     * @return optional JetStream durable consumer name used for subscriptions
     */
    public String getDurableName() {
        return this.durableName;
    }

    /**
     * @param durableName optional JetStream durable consumer name used for subscriptions
     */
    public void setDurableName(String durableName) {
        this.durableName = durableName;
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

    /**
     * @return optional JetStream consumer acknowledgement wait
     */
    public Duration getAckWait() {
        return this.ackWait;
    }

    /**
     * @param ackWait optional JetStream consumer acknowledgement wait
     */
    public void setAckWait(Duration ackWait) {
        this.ackWait = ackWait;
    }

    /**
     * @return optional maximum delivery attempts for a JetStream consumer
     */
    public Long getMaxDeliver() {
        return this.maxDeliver;
    }

    /**
     * @param maxDeliver optional maximum delivery attempts for a JetStream consumer
     */
    public void setMaxDeliver(Long maxDeliver) {
        this.maxDeliver = maxDeliver;
    }

    /**
     * @return optional maximum unacknowledged messages for a JetStream consumer
     */
    public Long getMaxAckPending() {
        return this.maxAckPending;
    }

    /**
     * @param maxAckPending optional maximum unacknowledged messages for a JetStream consumer
     */
    public void setMaxAckPending(Long maxAckPending) {
        this.maxAckPending = maxAckPending;
    }

    /**
     * @return optional JetStream consumer delivery policy
     */
    public DeliverPolicy getDeliverPolicy() {
        return this.deliverPolicy;
    }

    /**
     * @param deliverPolicy optional JetStream consumer delivery policy
     */
    public void setDeliverPolicy(DeliverPolicy deliverPolicy) {
        this.deliverPolicy = deliverPolicy;
    }

    /**
     * @return optional JetStream consumer replay policy
     */
    public ReplayPolicy getReplayPolicy() {
        return this.replayPolicy;
    }

    /**
     * @param replayPolicy optional JetStream consumer replay policy
     */
    public void setReplayPolicy(ReplayPolicy replayPolicy) {
        this.replayPolicy = replayPolicy;
    }

    /**
     * @return optional JetStream consumer idle heartbeat
     */
    public Duration getIdleHeartbeat() {
        return this.idleHeartbeat;
    }

    /**
     * @param idleHeartbeat optional JetStream consumer idle heartbeat
     */
    public void setIdleHeartbeat(Duration idleHeartbeat) {
        this.idleHeartbeat = idleHeartbeat;
    }

    /**
     * @return optional JetStream push consumer flow-control flag
     */
    public Boolean getFlowControl() {
        return this.flowControl;
    }

    /**
     * @param flowControl optional JetStream push consumer flow-control flag
     */
    public void setFlowControl(Boolean flowControl) {
        this.flowControl = flowControl;
    }

    /**
     * @return optional inactive threshold for an ephemeral JetStream consumer
     */
    public Duration getInactiveThreshold() {
        return this.inactiveThreshold;
    }

    /**
     * @param inactiveThreshold optional inactive threshold for an ephemeral JetStream consumer
     */
    public void setInactiveThreshold(Duration inactiveThreshold) {
        this.inactiveThreshold = inactiveThreshold;
    }

    /**
     * @return optional maximum outstanding pull requests for a JetStream pull consumer
     */
    public Long getMaxPullWaiting() {
        return this.maxPullWaiting;
    }

    /**
     * @param maxPullWaiting optional maximum outstanding pull requests for a JetStream pull consumer
     */
    public void setMaxPullWaiting(Long maxPullWaiting) {
        this.maxPullWaiting = maxPullWaiting;
    }

    /**
     * @return optional maximum pull batch size for a JetStream pull consumer
     */
    public Long getMaxBatch() {
        return this.maxBatch;
    }

    /**
     * @param maxBatch optional maximum pull batch size for a JetStream pull consumer
     */
    public void setMaxBatch(Long maxBatch) {
        this.maxBatch = maxBatch;
    }

    /**
     * @return optional maximum pull byte size for a JetStream pull consumer
     */
    public Long getMaxBytes() {
        return this.maxBytes;
    }

    /**
     * @param maxBytes optional maximum pull byte size for a JetStream pull consumer
     */
    public void setMaxBytes(Long maxBytes) {
        this.maxBytes = maxBytes;
    }

    /**
     * @return optional ordered-consumer flag for JetStream push consumers
     */
    public Boolean getOrdered() {
        return this.ordered;
    }

    /**
     * @param ordered optional ordered-consumer flag for JetStream push consumers
     */
    public void setOrdered(Boolean ordered) {
        this.ordered = ordered;
    }

    /**
     * @return optional timeout used by JetStream polled consumers when fetching one message
     */
    public Duration getPollTimeout() {
        return this.pollTimeout;
    }

    /**
     * @param pollTimeout optional timeout used by JetStream polled consumers when fetching one message
     */
    public void setPollTimeout(Duration pollTimeout) {
        this.pollTimeout = pollTimeout;
    }
}
