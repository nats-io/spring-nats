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

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.cloud.stream.binder.PollableMessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.MessageChannel;

@SpringBootApplication
@EnableBinding(PollingSample.PolledProcessor.class)
public class PollingSample {

	private static final Log logger = LogFactory.getLog(PollingSample.class);
	public static final ExecutorService exec = Executors.newSingleThreadExecutor();

	public static void main(String[] args) {
		SpringApplication.run(PollingSample.class, args);
	}

	@Bean
	public ApplicationRunner runner(PollableMessageSource input, MessageChannel output) {
		return args -> {
			exec.execute(() -> {
				boolean result = false;
				while (true) {
					result = input.poll(message -> {
						byte[] bytes = (byte[]) message.getPayload();
						String val = new String(bytes, StandardCharsets.UTF_8);
						logger.info("received message " + val);
					});
				}
			});
		};
	}

	public interface PolledProcessor {
		@Input
		PollableMessageSource input();

		@Output
		MessageChannel output();
	}
}
