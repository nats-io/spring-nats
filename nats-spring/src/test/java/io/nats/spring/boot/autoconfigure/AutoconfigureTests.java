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

package io.nats.spring.boot.autoconfigure;

import io.nats.client.Connection;
import org.junit.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.io.IOException;
import java.time.Duration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class AutoconfigureTests {
    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(NatsAutoConfiguration.class));

    @Test
    public void testDefaultConnection() throws IOException, InterruptedException {
        try (NatsTestServer ts = new NatsTestServer()) {
            this.contextRunner.withPropertyValues("nats.spring.server=" + ts.getURI(),
                    "nats.spring.connectionTimeout=15s").run((context) -> {
                Connection conn = (Connection) context.getBean(Connection.class);
                assertNotNull(conn);
                assertTrue("Connected Status", Connection.Status.CONNECTED == conn.getStatus());
                assertEquals(ts.getURI(), conn.getConnectedUrl());
                assertEquals(Duration.ofSeconds(15), conn.getOptions().getConnectionTimeout());
            });
        }
    }

    @Test
    public void testSSLConnection() throws IOException, InterruptedException {
        try (NatsTestServer ts = new NatsTestServer("src/test/resources/tls.conf", false)) {
            this.contextRunner.withPropertyValues("nats.spring.server=" + ts.getURI(),
                    "nats.spring.connectionTimeout=15s",
                    "nats.spring.keystorepath=src/test/resources/keystore.jks",
                    "nats.spring.keystorepassword=password",
                    "nats.spring.truststorepath=src/test/resources/cacerts",
                    "nats.spring.truststorepassword=password").run((context) -> {
                Connection conn = (Connection) context.getBean(Connection.class);
                assertNotNull(conn);
                assertTrue("Connected Status", Connection.Status.CONNECTED == conn.getStatus());
                assertEquals(ts.getURI(), conn.getConnectedUrl());
                assertEquals(Duration.ofSeconds(15), conn.getOptions().getConnectionTimeout());
            });
        }
    }

    @Test(expected = org.springframework.beans.factory.BeanNotOfRequiredTypeException.class)
    public void testNoParamNoConnection() throws IOException, InterruptedException {
        try (NatsTestServer ts = new NatsTestServer()) {
            this.contextRunner.run((context) -> {
                Object o = context.getBean(Connection.class);
                assertNull(o);
            });
        }
    }

    @Test(expected = java.lang.IllegalStateException.class)
    public void testNoServer() throws IOException, InterruptedException {
        this.contextRunner.withPropertyValues("nats.spring.server=nodomain:3311").run((context) -> {
            Object o = context.getBean(Connection.class);
            assertNull(o);
        });
    }
}
