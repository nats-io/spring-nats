/*
 * Copyright 2015-2018 the original author or authors.
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

import java.util.HashSet;
import java.util.Set;

import org.springframework.cloud.stream.binder.AbstractTestBinder;
import org.springframework.cloud.stream.binder.Binding;
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.cloud.stream.binder.ExtendedProducerProperties;
import org.springframework.cloud.stream.binder.nats.properties.NatsConsumerProperties;
import org.springframework.cloud.stream.binder.nats.properties.NatsProducerProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.messaging.MessageChannel;

// import org.springframework.cloud.stream.binder.nats.properties.NatsCommonProperties;
// import org.springframework.cloud.stream.binder.nats.provisioning.NatsSubjectProvisioner;
// import org.springframework.util.StringUtils;
// import org.springframework.cloud.stream.binder.PollableSource;
// import org.springframework.messaging.MessageHandler;
/**
 * Test support class for {@link NatsMessageChannelBinder}.
 *
 * @author The NATS Authors
 */
// @checkstyle:off
public class NatsTestBinder extends AbstractTestBinder<NatsMessageChannelBinder, ExtendedConsumerProperties<NatsConsumerProperties>, ExtendedProducerProperties<NatsProducerProperties>> {

	// @checkstyle:on

	private final Set<String> prefixes = new HashSet<>();

	private final Set<String> queues = new HashSet<String>();

	//private final AnnotationConfigApplicationContext applicationContext;
	private final GenericApplicationContext applicationContext = null;

	public NatsTestBinder() {
		// this.applicationContext = new GenericApplicationContext();
		// NatsMessageChannel binder = new NatsMessageChannelBinder(null, natsChannelProvisioner, natsTemplate,
		// 		new NatsExtendedBindingProperties());
		// binder.setApplicationContext(this.applicationContext);
		// this.setBinder(binder);
	}

	public GenericApplicationContext getApplicationContext() {
		return this.applicationContext;
	}

	@Override
	public Binding<MessageChannel> bindConsumer(String name, String group,
			MessageChannel moduleInputChannel,
			ExtendedConsumerProperties<NatsConsumerProperties> properties) {
		captureConsumerResources(name, group, properties);
		return super.bindConsumer(name, group, moduleInputChannel, properties);
	}

	// @Override
	// public Binding<PollableSource<MessageHandler>> bindPollableConsumer(String name,
	// 		String group, PollableSource<MessageHandler> inboundBindTarget,
	// 		ExtendedConsumerProperties<NatsConsumerProperties> properties) {
	// 	captureConsumerResources(name, group, properties);
	// 	return super.bindPollableConsumer(name, group, inboundBindTarget, properties);
	// }

	private void captureConsumerResources(String name, String group,
		ExtendedConsumerProperties<NatsConsumerProperties> properties) {
		// String[] names = null;
		// if (group != null) {
		// 	if (properties.getExtension().isQueueNameGroupOnly()) {
		// 		this.queues.add(properties.getExtension().getPrefix() + group);
		// 	}
		// 	else {
		// 		if (properties.isMultiplex()) {
		// 			names = StringUtils.commaDelimitedListToStringArray(name);
		// 			for (String nayme : names) {
		// 				this.queues.add(properties.getExtension().getPrefix()
		// 						+ nayme.trim() + "." + group);
		// 			}
		// 		}
		// 		else {
		// 			this.queues.add(
		// 					properties.getExtension().getPrefix() + name + "." + group);
		// 		}
		// 	}
		// }
		// if (names != null) {
		// 	for (String nayme : names) {
		// 		this.exchanges.add(properties.getExtension().getPrefix() + nayme.trim());
		// 	}
		// }
		// else {
		// 	this.exchanges.add(properties.getExtension().getPrefix() + name);
		// }
		// this.prefixes.add(properties.getExtension().getPrefix());
	}

	@Override
	public Binding<MessageChannel> bindProducer(String name,
			MessageChannel moduleOutputChannel,
			ExtendedProducerProperties<NatsProducerProperties> properties) {
		// this.queues.add(properties.getExtension().getPrefix() + name + ".default");
		// this.exchanges.add(properties.getExtension().getPrefix() + name);
		// if (properties.getRequiredGroups() != null) {
		// 	for (String group : properties.getRequiredGroups()) {
		// 		if (properties.getExtension().isQueueNameGroupOnly()) {
		// 			this.queues.add(properties.getExtension().getPrefix() + group);
		// 		}
		// 		else {
		// 			this.queues.add(
		// 					properties.getExtension().getPrefix() + name + "." + group);
		// 		}
		// 	}
		// }
		// this.prefixes.add(properties.getExtension().getPrefix());
		return super.bindProducer(name, moduleOutputChannel, properties);
	}

	@Override
	public void cleanup() {
		this.applicationContext.close();
	}

	@Configuration
	@EnableIntegration
	static class Config {

	}

}
