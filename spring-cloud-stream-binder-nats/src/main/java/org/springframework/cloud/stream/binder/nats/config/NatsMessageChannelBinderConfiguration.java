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

package org.springframework.cloud.stream.binder.nats.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.binder.nats.NatsMessageChannelBinder;
import org.springframework.cloud.stream.binder.nats.NatsMessageSource;
import org.springframework.cloud.stream.binder.nats.properties.NatsBinderConfigurationProperties;
import org.springframework.cloud.stream.binder.nats.properties.NatsExtendedBindingProperties;
import org.springframework.cloud.stream.binder.nats.provisioning.NatsSubjectProvisioner;
import org.springframework.cloud.stream.config.MessageSourceCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.lang.Nullable;

// TODO:  Remove later after implementation
//import org.springframework.cloud.stream.config.ListenerContainerCustomizer;
//import org.springframework.cloud.stream.binder.nats.NatsMessageChannelBinder;
//import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
//import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
//import org.springframework.cloud.stream.config.MessageSourceCustomizer;

/**
 * Configuration class for NATS message channel binder.
 *
 * @author The NATS Authors
 */
@Configuration
@Import({ PropertyPlaceholderAutoConfiguration.class })
@EnableConfigurationProperties({ NatsBinderConfigurationProperties.class,
		NatsExtendedBindingProperties.class })
public class NatsMessageChannelBinderConfiguration {

	@Autowired
	private NatsBinderConfigurationProperties natsBinderConfigurationProperties;

	@Autowired
	private NatsExtendedBindingProperties natsExtendedBindingProperties;

	@Bean
	NatsMessageChannelBinder natsMessageChannelBinder(
			@Nullable MessageSourceCustomizer<NatsMessageSource> sourceCustomizer) {

		NatsMessageChannelBinder binder = new NatsMessageChannelBinder(
			natsBinderConfigurationProperties,
			provisioningProvider(),
			sourceCustomizer);
		/*
		binder.setAdminAddresses(this.natsBinderConfigurationProperties.getAdminAddresses());
		binder.setCompressingPostProcessor(gZipPostProcessor());
		binder.setDecompressingPostProcessor(deCompressingPostProcessor());
		binder.setNodes(this.natsBinderConfigurationProperties.getNodes());
		binder.setExtendedBindingProperties(this.natsExtendedBindingProperties);
		*/
		return binder;
	}

	@Bean
	NatsSubjectProvisioner provisioningProvider() {
		return new NatsSubjectProvisioner();
	}
}
