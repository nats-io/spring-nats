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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

@Component
@EnableScheduling
public class TimedSource {
    private static final Log logger = LogFactory.getLog(TimedSource.class);
    private AtomicLong counter = new AtomicLong(0);

    @Autowired
    private StreamBridge streamBridge;

    @Scheduled(fixedRate = 2000)
    public void tick() {
        String msg = "message " + counter.incrementAndGet();

        if (streamBridge == null) {
            logger.info("no output to send to - " + msg);
            return;
        }

        logger.info("sending - " + msg);
        streamBridge.send("timedchannel", MessageBuilder.withPayload(msg.getBytes(StandardCharsets.UTF_8)).build());
    }
}
