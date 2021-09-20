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

package io.nats.spring;

import java.nio.charset.StandardCharsets;

import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Processor;
import org.springframework.messaging.handler.annotation.SendTo;

@EnableBinding(Processor.class)
public class UpperCaseTransformer {
	@StreamListener(Processor.INPUT)
	@SendTo(Processor.OUTPUT)
	public Object transform(Object message) {
		if (message instanceof byte[]) {
			String value = new String((byte[]) message, StandardCharsets.UTF_8);
			message = value.toUpperCase().getBytes(StandardCharsets.UTF_8);
		}
		else if (message instanceof String) {
			message = ((String) message).toUpperCase();
		}
		return message;
	}
}
