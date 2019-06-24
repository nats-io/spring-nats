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

import java.time.Duration;

import io.nats.client.Connection;
import io.nats.client.Message;
import io.nats.client.Subscription;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.Lifecycle;
import org.springframework.integration.endpoint.AbstractMessageSource;

public class NatsMessageSource extends AbstractMessageSource<Object> implements Lifecycle {
	private static final Log logger = LogFactory.getLog(NatsMessageHandler.class);

	private NatsConsumerDestination destination;
	private Connection connection;
	private Subscription sub;

	public NatsMessageSource(NatsConsumerDestination destination, Connection nc) {
		this.destination = destination;
		this.connection = nc;
	}

	@Override
	protected Object doReceive() {
		if (this.sub == null) {
			return null;
		}

		try {
			Message m = this.sub.nextMessage(Duration.ZERO);

			if (m != null) {
				return m.getData();
			}
		}
		catch (InterruptedException exp) {
			logger.info("wait for message interrupted");
		}

		return null;
	}

	@Override
	public boolean isRunning() {
		return this.sub != null;
	}

	@Override
	public void start() {
		if (this.sub != null) {
			return;
		}

		String sub = this.destination.getSubject();
		String queue = this.destination.getQueueGroup();

		if (queue != null && queue.length() > 0) {
			this.sub = this.connection.subscribe(sub, queue);
		}
		else {
			this.sub = this.connection.subscribe(sub);
		}
	}

	@Override
	public void stop() {
		if (this.sub == null) {
			return;
		}

		this.sub.unsubscribe();
	}

	@Override
	public String getComponentType() {
		return "nats:message-source";
	}
}
