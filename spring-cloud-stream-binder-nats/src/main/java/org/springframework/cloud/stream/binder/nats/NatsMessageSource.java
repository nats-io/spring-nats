/*
 * Copyright 2013-2019 the original author or authors.
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
import io.nats.client.Nats;
import io.nats.client.Subscription;

import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.cloud.stream.binder.nats.properties.NatsConsumerProperties;
import org.springframework.integration.endpoint.AbstractMessageSource;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.Assert;


/**
 * A pollable {@link org.springframework.integration.core.MessageSource} for NATS.
 */
public class NatsMessageSource extends AbstractMessageSource<Object> {
	private final String subject;
	private final String group;
	private Connection connection;
	private Subscription subscription;
	private ExtendedConsumerProperties<NatsConsumerProperties> properties;

	public NatsMessageSource(String subject, String group, ExtendedConsumerProperties<NatsConsumerProperties> properties) {
		Assert.notNull(subject, "'queue' cannot be null");
		this.subject = subject;
		this.group = group;
		this.properties = properties;

		// FIXME - parse options from the config
		try {
			this.connection = Nats.connect("localhost");
		}
		catch (Exception ex) {
			// TODO = log?
		}
	}


	@Override
	public String getComponentType() {
		return "nats:message-source";
	}

	@Override
	protected AbstractIntegrationMessageBuilder<Object> doReceive() {

		// TODO:  Get NATS message (from where?), finish this...
		io.nats.client.Message msg = null;

		String replySubject = msg.getReplyTo();
		byte[] payload = null;
		AbstractIntegrationMessageBuilder<Object> builder = getMessageBuilderFactory().withPayload((Object) payload)
					.setHeader(MessageHeaders.REPLY_CHANNEL, replySubject);
		return builder;

		//
		// TODO - get NATS messages  E.g. Rabbit Code for this...
		//
		// subscription.NextMessage();
		/*
		try {
			Message resp = channel.basicGet(this.queue, false);
			if (resp == null) {
				RabbitUtils.closeChannel(channel);
				RabbitUtils.closeConnection(connection);
				return null;
			}
			AcknowledgmentCallback callback = this.ackCallbackFactory
					.createCallback(new AmqpAckInfo(connection, channel, this.transacted, resp));
			MessageProperties messageProperties = this.propertiesConverter.toMessageProperties(resp.getProps(),
					resp.getEnvelope(), StandardCharsets.UTF_8.name());
			messageProperties.setConsumerQueue(this.queue);
			Map<String, Object> headers = this.headerMapper.toHeadersFromRequest(messageProperties);
			org.springframework.amqp.core.Message amqpMessage = new org.springframework.amqp.core.Message(resp.getBody(), messageProperties);
			Object payload;
			if (this.batchingStrategy.canDebatch(messageProperties)) {
				List<Object> payloads = new ArrayList<>();
				this.batchingStrategy.deBatch(amqpMessage, fragment -> payloads
						.add(this.messageConverter.fromMessage(fragment)));
				payload = payloads;
			}
			else {
				payload = this.messageConverter.fromMessage(amqpMessage);
			}
			AbstractIntegrationMessageBuilder<Object> builder = getMessageBuilderFactory().withPayload(payload)
					.copyHeaders(headers)
					.setHeader(IntegrationMessageHeaderAccessor.ACKNOWLEDGMENT_CALLBACK, callback);
			if (this.rawMessageHeader) {
				builder.setHeader(AmqpMessageHeaderErrorMessageStrategy.AMQP_RAW_MESSAGE, amqpMessage);
				builder.setHeader(IntegrationMessageHeaderAccessor.SOURCE_DATA, amqpMessage);
			}
			return builder;
		}
		catch (IOException e) {
			RabbitUtils.closeChannel(channel);
			RabbitUtils.closeConnection(connection);
			throw RabbitExceptionTranslator.convertRabbitAccessException(e);
		}
		*/
	}
}

