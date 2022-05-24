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

import io.nats.client.Connection;
import io.nats.client.ConnectionListener;
import io.nats.client.Consumer;
import io.nats.client.ErrorListener;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ConnectErrorSample {

    public static void main(String[] args) {
        SpringApplication.run(ConnectErrorSample.class, args);
    }

    @Bean
    public ConnectionListener createConnectionListener() {
        return new ConnectionListener() {
            @Override
            public void connectionEvent(Connection conn, Events type) {
                System.out.println("## Custom status change " + type);
            }
        };
    }

    @Bean
    public ErrorListener createErrorListener() {
        return new ErrorListener() {
            @Override
            public void slowConsumerDetected(Connection conn, Consumer consumer) {
                System.out.println("## slow consumer detected");
            }

            @Override
            public void exceptionOccurred(Connection conn, Exception exp) {
                System.out.println("## exception occurred");
                exp.printStackTrace();
            }

            @Override
            public void errorOccurred(Connection conn, String error) {
                System.out.println("## error occurred " + error);
            }
        };
    }
}
