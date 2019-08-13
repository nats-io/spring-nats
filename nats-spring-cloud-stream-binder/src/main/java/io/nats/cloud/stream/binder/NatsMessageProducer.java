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

package io.nats.cloud.stream.binder;

import java.util.HashMap;
import java.util.Map;

import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.Lifecycle;
import org.springframework.integration.core.MessageProducer;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;

/**
 * MessageProducer for NATS connections.
 */
public class NatsMessageProducer implements MessageProducer, Lifecycle {
	private static final Log logger = LogFactory.getLog(NatsMessageProducer.class);

	/**
	 * The NATS subject for incoming message is stored in the SUBJECT header.
	 */
	public static final String SUBJECT = "subject";

	/**
	 * If an incoming message has a reply to subject, that will be stored in the REPLY_TO header for propogation to the NatsMessageSource.
	 */
	public static final String REPLY_TO = "reply_to";

	private NatsConsumerDestination destination;
	private Connection connection;
	private MessageChannel output;
	private Dispatcher dispatcher;

	/**
	 * Create a message producer. Once started the producer will use a dispatcher, and the associated thread, to
	 * listen for and handle incoming messages.
	 * @param destination where to subscribe
	 * @param nc NATS connection
	 */
	public NatsMessageProducer(NatsConsumerDestination destination, Connection nc) {
		this.destination = destination;
		this.connection = nc;
	}

	@Override
	public void setOutputChannel(MessageChannel outputChannel) {
		this.output = outputChannel;
	}

	@Override
	public MessageChannel getOutputChannel() {
		return this.output;
	}

	@Override
	public boolean isRunning() {
		return this.dispatcher != null;
	}

	@Override
	public void start() {
		if (this.dispatcher != null) {
			return;
		}

		this.dispatcher = this.connection.createDispatcher((msg) -> {

			if (this.output == null) {
				logger.warn("skipping message, no output channel set for " + this.destination.getName());
				return;
			}

			try {
				Map<String, Object> headers = new HashMap<>();
				headers.put(SUBJECT, msg.getSubject());
				headers.put(REPLY_TO, msg.getReplyTo());
				GenericMessage<byte[]> m = new GenericMessage<byte[]>(msg.getData(), headers);
				this.output.send(m);
			}
			catch (Exception e) {
				logger.warn("exception sending message to output channel", e);
			}
		});

		String sub = this.destination.getSubject();
		String queue = this.destination.getQueueGroup();

		if (queue != null && queue.length() > 0) {
			this.dispatcher.subscribe(sub, queue);
		}
		else {
			this.dispatcher.subscribe(sub);
		}
	}

	@Override
	public void stop() {
		if (this.dispatcher == null) {
			return;
		}

		this.connection.closeDispatcher(this.dispatcher);
		this.dispatcher = null;
	}
}
