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

package org.springframework.cloud.stream.binder.nats;

import io.nats.client.Connection;
import io.nats.client.NUID;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.stream.binder.ConsumerProperties;
import org.springframework.cloud.stream.binder.ProducerProperties;
import org.springframework.cloud.stream.provisioning.ConsumerDestination;
import org.springframework.cloud.stream.provisioning.ProducerDestination;
import org.springframework.cloud.stream.provisioning.ProvisioningException;
import org.springframework.cloud.stream.provisioning.ProvisioningProvider;

public class NatsChannelProvisioner implements ProvisioningProvider<ConsumerProperties, ProducerProperties> {
	private static final Log logger = LogFactory.getLog(NatsChannelProvisioner.class);

	private final Connection connection;

	public NatsChannelProvisioner(Connection nc) {
		this.connection = nc;
	}

	public Connection getConnection() {
		return this.connection;
	}

	@Override
	public ProducerDestination provisionProducerDestination(String subject, ProducerProperties properties)
			throws ProvisioningException {
		return new NatsProducerDestination(subject);
	}

	@Override
	public ConsumerDestination provisionConsumerDestination(String subject, String group, ConsumerProperties properties)
			throws ProvisioningException {
		String subscriptionName;

		if (group != null && group.length() > 0) {
			subscriptionName = subject + "#" + group;
		}
		else {
			subscriptionName = "anonymous#" + subject + "#" + NUID.nextGlobal();
		}

		return new NatsConsumerDestination(subscriptionName);
	}
}
