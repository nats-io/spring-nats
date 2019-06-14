/*
 * Copyright 2016-2018 the original author or authors.
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

package org.springframework.cloud.stream.binder.nats.provisioning;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.cloud.stream.binder.ExtendedProducerProperties;
import org.springframework.cloud.stream.binder.nats.properties.NatsConsumerProperties;
import org.springframework.cloud.stream.binder.nats.properties.NatsProducerProperties;
import org.springframework.cloud.stream.provisioning.ConsumerDestination;
import org.springframework.cloud.stream.provisioning.ProducerDestination;
import org.springframework.cloud.stream.provisioning.ProvisioningProvider;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.util.Assert;
//import org.springframework.util.StringUtils;
//import org.springframework.context.ApplicationListener;
//import org.springframework.cloud.stream.provisioning.ProvisioningException;
//import org.springframework.cloud.stream.binder.nats.properties.NatsCommonProperties;

/**
 * NATS implementation for {@link ProvisioningProvider}.
 *
 * @author The NATS Authors
 */
// @checkstyle:off
public class NatsSubjectProvisioner implements ProvisioningProvider<ExtendedConsumerProperties<NatsConsumerProperties>,ExtendedProducerProperties<NatsProducerProperties>> {

	// @checkstyle:on

	protected final Log logger = LogFactory.getLog(getClass());

	private final GenericApplicationContext autoDeclareContext = new GenericApplicationContext();

	public NatsSubjectProvisioner() {
		this.autoDeclareContext.refresh();
	}

	@Override
	public ProducerDestination provisionProducerDestination(String name,
			ExtendedProducerProperties<NatsProducerProperties> producerProperties) {
		return new NatsProducerDestination(name);
	}

	@Override
	public ConsumerDestination provisionConsumerDestination(String name, String group,
			ExtendedConsumerProperties<NatsConsumerProperties> properties) {
		return new NatsConsumerDestination(name, group);
	}


	public static String applyPrefix(String prefix, String name) {
		return prefix + name;
	}

	public void cleanAutoDeclareContext(ConsumerDestination destination,
			ExtendedConsumerProperties<NatsConsumerProperties> consumerProperties) {
		synchronized (this.autoDeclareContext) {
			removeSingleton(destination.getName());
		}
	}

	private void removeSingleton(String name) {
		if (this.autoDeclareContext.containsBean(name)) {
			ConfigurableListableBeanFactory beanFactory = this.autoDeclareContext
					.getBeanFactory();
			if (beanFactory instanceof DefaultListableBeanFactory) {
				((DefaultListableBeanFactory) beanFactory).destroySingleton(name);
			}
		}
	}

	/*
	@Override
	public void onApplicationEvent(DeclarationExceptionEvent event) {
		this.notOurAdminException = true; // our admin doesn't have an event publisher
	}
	*/

	private static final class NatsProducerDestination implements ProducerDestination {

		private final String subject;

		NatsProducerDestination(String subject) {
			this.subject = subject;
		}

		@Override
		public String getName() {
			return subject;
		}

		@Override
		public String getNameForPartition(int partition) {
			return subject;
		}

		@Override
		public String toString() {
			return "NatsProducerDestination{ subject=" + subject + "}";
		}
	}

	private static final class NatsConsumerDestination implements ConsumerDestination {

		private final String subject;
		private final String group;

		NatsConsumerDestination(String subject, String group) {
			Assert.notNull(subject, "subject must not be null");
			this.subject = subject;
			this.group = group;
		}

		@Override
		public String toString() {
			return "NatsConsumerDestination{" + "subject=" + subject + ", group="
					+ group + '}';
		}

		@Override
		public String getName() {
			return this.subject;
		}
	}
}
