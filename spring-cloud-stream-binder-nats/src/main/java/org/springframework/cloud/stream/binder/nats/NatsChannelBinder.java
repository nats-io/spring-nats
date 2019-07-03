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
import io.nats.client.ConnectionListener;
import io.nats.client.Consumer;
import io.nats.client.ErrorListener;
import io.nats.client.Nats;
import io.nats.client.Options;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.autoconfigure.nats.NatsProperties;
import org.springframework.cloud.stream.binder.AbstractMessageChannelBinder;
import org.springframework.cloud.stream.binder.BinderSpecificPropertiesProvider;
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.cloud.stream.binder.ExtendedProducerProperties;
import org.springframework.cloud.stream.binder.ExtendedPropertiesBinder;
import org.springframework.cloud.stream.binder.nats.properties.NatsBinderConfigurationProperties;
import org.springframework.cloud.stream.binder.nats.properties.NatsConsumerProperties;
import org.springframework.cloud.stream.binder.nats.properties.NatsExtendedBindingProperties;
import org.springframework.cloud.stream.binder.nats.properties.NatsProducerProperties;
import org.springframework.cloud.stream.provisioning.ConsumerDestination;
import org.springframework.cloud.stream.provisioning.ProducerDestination;
import org.springframework.integration.core.MessageProducer;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

public class NatsChannelBinder extends
		AbstractMessageChannelBinder<ExtendedConsumerProperties<NatsConsumerProperties>, ExtendedProducerProperties<NatsProducerProperties>, NatsChannelProvisioner>
		implements ExtendedPropertiesBinder<MessageChannel, NatsConsumerProperties, NatsProducerProperties> {
			private static final Log logger = LogFactory.getLog(NatsChannelBinder.class);
	private final NatsExtendedBindingProperties bindingProperties;
	private NatsBinderConfigurationProperties properties;
	private NatsProperties natsProperties;
	private Connection connection;

	public NatsChannelBinder(NatsExtendedBindingProperties bindingProperties,
			NatsBinderConfigurationProperties properties,
			NatsProperties natsProperties,
			NatsChannelProvisioner provisioningProvider,
			ConnectionListener connectionListener,
			ErrorListener errorListener) {
		super(null, provisioningProvider); // null for headers to embed
		this.bindingProperties = bindingProperties;
		this.properties = properties;
		this.natsProperties = natsProperties;

		try {
			Options.Builder builder = null;
			String bindingServer = (this.properties != null) ? this.properties.getServer() : null;
			String globalServer = (this.natsProperties != null) ? this.natsProperties.getServer() : null;

			// Use the binder properties first, if they don't have a server, try the global
			if (bindingServer != null && bindingServer.length() > 0) {
				logger.info("binder connecting to nats with named properties " + this.properties);
				builder = this.properties.toOptionsBuilder();
			}
			else if (globalServer != null && globalServer.length() > 0) {
				logger.info("binder connecting to nats with global properties " + this.natsProperties);
				builder = this.natsProperties.toOptionsBuilder();
			}
			else {
				this.connection = null;
				logger.info("unable to connect from binder to NATS no server properties where found");
				return;
			}

			if (connectionListener != null) {
				builder = builder.connectionListener(connectionListener);
			}
			else {
				builder = builder.connectionListener(new ConnectionListener() {
					public void connectionEvent(Connection conn, Events type) {
							logger.info("NATS connection status changed " + type);
					}
				});
			}

			if (errorListener != null) {
				builder = builder.errorListener(errorListener);
			}
			else {
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
			}

			this.connection = Nats.connect(builder.build());
		}
		catch (Exception e) {
			logger.info("exception connecting binder to NATS", e);
			this.connection = null;
		}

		if (this.connection == null) {
			logger.info("unable to connect from binder to NATS");
		}
	}

	public Connection getConnection() {
		return this.connection;
	}

	@Override
	protected MessageHandler createProducerMessageHandler(ProducerDestination destination,
			ExtendedProducerProperties<NatsProducerProperties> producerProperties, MessageChannel errorChannel) {
		return new NatsMessageHandler(destination.getName(), this.connection);
	}

	@Override
	protected MessageProducer createConsumerEndpoint(ConsumerDestination destination, String group,
			ExtendedConsumerProperties<NatsConsumerProperties> properties) {
		return new NatsMessageProducer((NatsConsumerDestination) destination, this.connection);
	}

	@Override
	protected PolledConsumerResources createPolledConsumerResources(String name, String group,
			ConsumerDestination destination, ExtendedConsumerProperties<NatsConsumerProperties> consumerProperties) {
		return new PolledConsumerResources(
				new NatsMessageSource((NatsConsumerDestination) destination, this.connection),
				registerErrorInfrastructure(destination, group, consumerProperties, true));
	}

	@Override
	public NatsConsumerProperties getExtendedConsumerProperties(String channelName) {
		return bindingProperties.getExtendedConsumerProperties(channelName);
	}

	@Override
	public NatsProducerProperties getExtendedProducerProperties(String channelName) {
		return bindingProperties.getExtendedProducerProperties(channelName);
	}

	@Override
	public String getDefaultsPrefix() {
		return bindingProperties.getDefaultsPrefix();
	}

	@Override
	public Class<? extends BinderSpecificPropertiesProvider> getExtendedPropertiesEntryClass() {
		return bindingProperties.getExtendedPropertiesEntryClass();
	}
}
