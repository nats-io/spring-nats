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

import java.io.IOException;
import java.util.Collections;

import io.nats.client.ConnectionListener;
import io.nats.client.ErrorListener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.nats.NatsAutoConfiguration;
import org.springframework.boot.autoconfigure.nats.NatsProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.cloud.stream.binder.nats.properties.NatsBinderConfigurationProperties;
import org.springframework.cloud.stream.binder.nats.properties.NatsExtendedBindingProperties;
import org.springframework.cloud.stream.config.BindingHandlerAdvise.MappingsProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({ NatsAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class })
@EnableConfigurationProperties({NatsExtendedBindingProperties.class, NatsBinderConfigurationProperties.class})
public class NatsChannelBinderConfiguration {

	@Autowired
	private ConnectionListener connectionListener;

	@Autowired
	private ErrorListener errorListener;

	@Autowired
	private NatsProperties natsProperties;

	@Autowired
	private NatsBinderConfigurationProperties natsBinderConfigurationProperties;

	@Autowired
	private NatsExtendedBindingProperties natsExtendedBindingProperties;

	public NatsBinderConfigurationProperties getNatsBinderConfigurationProperties() {
		return this.natsBinderConfigurationProperties;
	}

	public void setNatsBinderConfigurationProperties(NatsBinderConfigurationProperties natsBinderConfigurationProperties) {
		this.natsBinderConfigurationProperties = natsBinderConfigurationProperties;
	}

	public NatsExtendedBindingProperties getNatsExtendedBindingProperties() {
		return this.natsExtendedBindingProperties;
	}

	public void setNatsExtendedBindingProperties(NatsExtendedBindingProperties natsExtendedBindingProperties) {
		this.natsExtendedBindingProperties = natsExtendedBindingProperties;
	}

	public NatsProperties getNatsProperties() {
		return this.natsProperties;
	}

	public void setNatsProperties(NatsProperties natsProperties) {
		this.natsProperties = natsProperties;
	}

	@Bean
	public NatsChannelProvisioner natsChannelProvisioner() {
		return new NatsChannelProvisioner();
	}

	@Bean
	public NatsChannelBinder natsBinder(NatsChannelProvisioner natsProvisioner) throws IOException, InterruptedException {
		NatsChannelBinder binder = new NatsChannelBinder(this.natsExtendedBindingProperties,
										this.natsBinderConfigurationProperties,
										this.natsProperties, natsProvisioner,
										this.connectionListener,
										this.errorListener);
		return binder.getConnection() != null ? binder : null;
	}

	@Bean
	public MappingsProvider natsExtendedPropertiesDefaultMappingsProvider() {
		return () -> Collections.singletonMap(
				ConfigurationPropertyName.of("spring.cloud.stream.nats"),
				ConfigurationPropertyName.of("spring.cloud.stream.nats.default"));
	}
}
