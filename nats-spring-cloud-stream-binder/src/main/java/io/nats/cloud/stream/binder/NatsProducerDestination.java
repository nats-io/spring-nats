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

import org.springframework.cloud.stream.provisioning.ProducerDestination;

import java.util.Objects;

/**
 * NATS uses subjects for sending and receiving. While partitions are not used
 * this class can generate a partition-based name for compatibility with the binder
 * API.
 */
public class NatsProducerDestination implements ProducerDestination {
    private String name;

    /**
     * Create a new producer destination with the provided subject name.
     *
     * @param name subject name; must not be {@code null}
     * @throws NullPointerException if {@code name} is {@code null}
     */
    public NatsProducerDestination(String name) {
        this.name = Objects.requireNonNull(name, "name must not be null");
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getNameForPartition(int partition) {
        return this.name + "-" + partition;
    }
}
