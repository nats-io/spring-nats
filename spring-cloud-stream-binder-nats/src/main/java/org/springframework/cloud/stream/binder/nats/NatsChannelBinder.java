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

import org.springframework.cloud.stream.binder.AbstractMessageChannelBinder;
import org.springframework.cloud.stream.binder.ConsumerProperties;
import org.springframework.cloud.stream.binder.ProducerProperties;
import org.springframework.cloud.stream.provisioning.ConsumerDestination;
import org.springframework.cloud.stream.provisioning.ProducerDestination;
import org.springframework.integration.core.MessageProducer;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

public class NatsChannelBinder extends
		AbstractMessageChannelBinder<ConsumerProperties, ProducerProperties, NatsChannelProvisioner> {

	private final NatsChannelProvisioner provisioner;

	public NatsChannelBinder(NatsChannelProvisioner provisioningProvider) {
		super(null, provisioningProvider);
		this.provisioner = provisioningProvider;
	}

	@Override
	protected MessageHandler createProducerMessageHandler(ProducerDestination destination,
			ProducerProperties producerProperties, MessageChannel errorChannel) {
		/* TODO PubSubMessageHandler messageHandler = new PubSubMessageHandler(this.pubSubTemplate, destination.getName());
		messageHandler.setBeanFactory(getBeanFactory());
		return messageHandler; */
		return null;
	}

	@Override
	protected MessageProducer createConsumerEndpoint(ConsumerDestination destination, String group,
			ConsumerProperties properties) {

		// TODO return new PubSubInboundChannelAdapter(this.pubSubTemplate, destination.getName());
		return null;
	}

	@Override
	protected void afterUnbindConsumer(ConsumerDestination destination, String group,
			ConsumerProperties consumerProperties) {
		super.afterUnbindConsumer(destination, group, consumerProperties);
		// TODO clean up
	}

	@Override
	protected PolledConsumerResources createPolledConsumerResources(String name, String group,
			ConsumerDestination destination, ConsumerProperties consumerProperties) {
		/* return new PolledConsumerResources(new PubSubMessageSource(this.pubSubTemplate, destination.getName()),
				registerErrorInfrastructure(destination, group, consumerProperties, true)); */
		return null;
	}
}
