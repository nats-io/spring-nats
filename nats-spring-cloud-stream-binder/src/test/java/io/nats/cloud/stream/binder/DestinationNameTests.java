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

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.cloud.stream.provisioning.ProducerDestination;

public class DestinationNameTests {
    @Test
    public void testProducerNames() {
        String subject = "alpha";
        NatsChannelProvisioner provisioner = new NatsChannelProvisioner();
        ProducerDestination dest = provisioner.provisionProducerDestination(subject, null);

        assertEquals(subject, dest.getName());
        assertEquals(subject + "-1", dest.getNameForPartition(1));
    }

    @Test
    public void testConsumerNames() {
        String subject = "alpha";
        String group = "beta";
        NatsChannelProvisioner provisioner = new NatsChannelProvisioner();
        NatsConsumerDestination dest = (NatsConsumerDestination) provisioner.provisionConsumerDestination(subject, group, null);

        assertEquals(subject, dest.getSubject());
        assertEquals(group, dest.getQueueGroup());

        dest = (NatsConsumerDestination) provisioner.provisionConsumerDestination(subject, null, null);

        assertEquals(subject, dest.getSubject());
        assertEquals("", dest.getQueueGroup());
    }
}