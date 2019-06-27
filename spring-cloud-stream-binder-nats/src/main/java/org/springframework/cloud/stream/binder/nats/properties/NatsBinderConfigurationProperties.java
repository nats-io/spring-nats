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

package org.springframework.cloud.stream.binder.nats.properties;

import io.nats.client.Options;

import org.springframework.boot.autoconfigure.nats.NatsProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.cloud.stream.nats.binder")
public class NatsBinderConfigurationProperties {

	private final NatsProperties natsProperties;

	public NatsBinderConfigurationProperties(NatsProperties natsProperties) {

		if (natsProperties == null) {
			throw new IllegalArgumentException("nats properties cannot be null for a binder");
		}

		this.natsProperties = natsProperties;
	}

	public Options toOptions() {
		return this.natsProperties.toOptions();
	}

	@Override
	public String toString() {
		return this.natsProperties.toString();
	}
}
