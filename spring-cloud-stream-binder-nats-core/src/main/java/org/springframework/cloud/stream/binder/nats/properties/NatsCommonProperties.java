/*
 * Copyright 2017-2018 the original author or authors.
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

/**
 * @author The NATS Authors
 *
 */
public abstract class NatsCommonProperties {


	/**
	 * maximum number of messages in the queue.
	 */
	private Integer maxLength;

	/**
	 * maximum number of total bytes in the queue from all messages.
	 */
	private Integer maxLengthBytes;

	/**
	 * action when maxLength or maxLengthBytes is exceeded.
	 */
	private String dlqOverflowBehavior;

	public Integer getMaxLength() {
		return this.maxLength;
	}

	public void setMaxLength(Integer maxLength) {
		this.maxLength = maxLength;
	}

	public Integer getMaxLengthBytes() {
		return this.maxLengthBytes;
	}

	public void setMaxLengthBytes(Integer maxLengthBytes) {
		this.maxLengthBytes = maxLengthBytes;
	}

}
