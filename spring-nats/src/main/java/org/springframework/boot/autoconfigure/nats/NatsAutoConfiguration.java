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

package org.springframework.boot.autoconfigure.nats;

import java.io.IOException;

import io.nats.client.Connection;
import io.nats.client.Nats;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for NATS.
 */
@ConditionalOnClass({ Connection.class })
@EnableConfigurationProperties(NatsProperties.class)
public class NatsAutoConfiguration {
	private static final Log logger = LogFactory.getLog(NatsAutoConfiguration.class);

	@Bean
	@ConditionalOnMissingBean
	public Connection natsConnection(NatsProperties properties) throws IOException, InterruptedException {
		Connection nc = null;
		String serverProp = (properties != null) ? properties.getServer() : null;

		if (serverProp == null || serverProp.length() == 0) {
			return null;
		}

		try {
			logger.info("autoconnecting to NATS with properties - " + properties);
			nc = Nats.connect(properties.toOptions());
		}
		catch (Exception e) {
			logger.info("error connecting to nats", e);
			throw e;
		}
		return nc;
	}

}
