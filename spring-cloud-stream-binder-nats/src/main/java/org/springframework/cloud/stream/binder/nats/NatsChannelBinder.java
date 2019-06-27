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

import io.nats.client.Connection;
import io.nats.client.ConnectionListener;
import io.nats.client.Consumer;
import io.nats.client.ErrorListener;
import io.nats.client.Nats;
import io.nats.client.Options;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.autoconfigure.nats.NatsProperties;
import org.springframework.cloud.stream.binder.AbstractMessageChannelBinder;
import org.springframework.cloud.stream.binder.ConsumerProperties;
import org.springframework.cloud.stream.binder.ProducerProperties;
import org.springframework.cloud.stream.provisioning.ConsumerDestination;
import org.springframework.cloud.stream.provisioning.ProducerDestination;
import org.springframework.integration.core.MessageProducer;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;


public class NatsChannelBinder
		extends AbstractMessageChannelBinder<ConsumerProperties, ProducerProperties, NatsChannelProvisioner> {
	private static final Log logger = LogFactory.getLog(NatsChannelBinder.class);
	private final NatsProperties properties;
	private Connection connection;

	public NatsChannelBinder(NatsProperties natsProperties, NatsChannelProvisioner provisioningProvider) throws IOException, InterruptedException {
		super(null, provisioningProvider);
		this.properties = natsProperties;

		try {
			System.out.println("#### Connecting to nats " + this.properties);
			Options.Builder builder = properties.toOptionsBuilder();

			builder = builder.connectionListener(new ConnectionListener() {
				public void connectionEvent(Connection conn, Events type) {
						logger.info("NATS connection status changed " + type);
				}
			});

			builder = builder.errorListener(new ErrorListener() {
				@Override
				public void slowConsumerDetected(Connection conn, Consumer consumer) {
					logger.info("NATS connection slow consumer detected");
				}

				@Override
				public void exceptionOccurred(Connection conn, Exception exp) {
					logger.info("NATS connection exception occurred", exp);
				}

				@Override
				public void errorOccurred(Connection conn, String error) {
					logger.info("NATS connection error occurred " + error);
				}
			});

			this.connection = Nats.connect(builder.build());
		}
		catch (Exception e) {
			logger.info("error connecting to nats", e);
			throw e;
		}
	}

	public Connection getConnection() {
		return this.connection;
	}

	@Override
	protected MessageHandler createProducerMessageHandler(ProducerDestination destination,
			ProducerProperties producerProperties, MessageChannel errorChannel) {
		return new NatsMessageHandler(destination.getName(), this.connection);
	}

	@Override
	protected MessageProducer createConsumerEndpoint(ConsumerDestination destination, String group,
			ConsumerProperties properties) {
		return new NatsMessageProducer((NatsConsumerDestination) destination, this.connection);
	}

	@Override
	protected PolledConsumerResources createPolledConsumerResources(String name, String group,
			ConsumerDestination destination, ConsumerProperties consumerProperties) {
		return new PolledConsumerResources(
				new NatsMessageSource((NatsConsumerDestination) destination, this.connection),
				registerErrorInfrastructure(destination, group, consumerProperties, true));
	}
}
