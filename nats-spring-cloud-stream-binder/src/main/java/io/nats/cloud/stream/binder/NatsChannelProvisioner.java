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

import io.nats.client.NUID;
import io.nats.cloud.stream.binder.properties.NatsConsumerProperties;
import io.nats.cloud.stream.binder.properties.NatsProducerProperties;
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.cloud.stream.binder.ExtendedProducerProperties;
import org.springframework.cloud.stream.provisioning.ConsumerDestination;
import org.springframework.cloud.stream.provisioning.ProducerDestination;
import org.springframework.cloud.stream.provisioning.ProvisioningException;
import org.springframework.cloud.stream.provisioning.ProvisioningProvider;

/**
 * Provisioners turn destination names into Destination objects. This is minimal for NATS where destination names are mapped to subjects.
 */
public class NatsChannelProvisioner implements ProvisioningProvider<ExtendedConsumerProperties<NatsConsumerProperties>, ExtendedProducerProperties<NatsProducerProperties>> {
    public NatsChannelProvisioner() {
    }

    @Override
    /**
     * @param subject for messages to go to
     * @param properties extended properties for the producer, unused
     * @return destination that will result in messages being sent to the provided subject
     */
    public ProducerDestination provisionProducerDestination(String subject, ExtendedProducerProperties<NatsProducerProperties> properties)
            throws ProvisioningException {
        return new NatsProducerDestination(subject);
    }

    @Override
    /**
     * @param subject to listen to messages on
     * @param group mapped to a queue group name in NATS
     * @param properties extended properties, unused
     * @return consumer desitation named based on the subject and group.
     */
    public ConsumerDestination provisionConsumerDestination(String subject, String group, ExtendedConsumerProperties<NatsConsumerProperties> properties)
            throws ProvisioningException {
        String subscriptionName;

        if (group != null && group.length() > 0) {
            subscriptionName = subject + "#" + group;
        } else {
            subscriptionName = "anonymous#" + subject + "#" + NUID.nextGlobal();
        }

        return new NatsConsumerDestination(subscriptionName);
    }
}
