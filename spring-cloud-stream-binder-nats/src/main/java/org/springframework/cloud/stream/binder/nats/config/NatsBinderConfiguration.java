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

package org.springframework.cloud.stream.binder.nats.config;

import java.util.Collections;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.cloud.stream.binder.Binder;
import org.springframework.cloud.stream.binder.nats.NatsMessageChannelBinder;
import org.springframework.cloud.stream.binder.nats.properties.NatsExtendedBindingProperties;
import org.springframework.cloud.stream.binder.nats.provisioning.NatsSubjectProvisioner;
import org.springframework.cloud.stream.config.BindingHandlerAdvise.MappingsProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Pub/Sub binder configuration.
 *
 * @author The NATS Authors
 */
@Configuration
@ConditionalOnMissingBean(Binder.class)
@EnableConfigurationProperties(NatsExtendedBindingProperties.class)
public class NatsBinderConfiguration {

	@Bean
	public NatsSubjectProvisioner natsSubjectProvisioner() {
		return new NatsSubjectProvisioner();
	}

	@Bean
	public NatsMessageChannelBinder natsBinder(
		NatsSubjectProvisioner natsSubjectProvisioner,
		NatsExtendedBindingProperties natsExtendedBindingProperties) {

		// TODO:  FIXME - fix property passing.  Not sure what we really need...
		return new NatsMessageChannelBinder(null, natsSubjectProvisioner, /*natsExtendedBindingProperties) TODO:  fixme*/ null);
	}

	@Bean
	public MappingsProvider NatsExtendedPropertiesDefaultMappingsProvider() {
		return () -> Collections.singletonMap(
				ConfigurationPropertyName.of("spring.cloud.stream.binder.nats.bindings"),
				ConfigurationPropertyName.of("spring.cloud.stream.binder.nats.default"));
	}
}
