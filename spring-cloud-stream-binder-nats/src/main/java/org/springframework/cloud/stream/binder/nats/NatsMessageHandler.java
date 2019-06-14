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

import org.springframework.cloud.stream.binder.ExtendedProducerProperties;
import org.springframework.cloud.stream.binder.nats.properties.NatsProducerProperties;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;


// import io.nats.client.Connection;
// import io.nats.client.Nats;
// import io.nats.client.Subscription;

// import org.springframework.integration.IntegrationMessageHeaderAccessor;
// import org.springframework.integration.acks.AcknowledgmentCallback;
// import org.springframework.integration.acks.AcknowledgmentCallbackFactory;
// import org.springframework.cloud.stream.binder.nats.properties.NatsConsumerProperties;
// import org.springframework.integration.endpoint.AbstractMessageSource;
// import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
// import org.springframework.util.Assert;

/**
 * A Message handler for NATS.
 */
public class NatsMessageHandler implements MessageHandler {

	private String subject;
	private ExtendedProducerProperties<NatsProducerProperties> properties;

	// TODO
	public NatsMessageHandler(String subject, ExtendedProducerProperties<NatsProducerProperties> properties) {
		this.subject = subject;
		this.properties = properties;
	}

	@Override
	public void handleMessage(Message<?> message)
			throws MessagingException {
			// TODO...  Publish?  Where to get connection?  Check out other implemtnations.
	}
}

