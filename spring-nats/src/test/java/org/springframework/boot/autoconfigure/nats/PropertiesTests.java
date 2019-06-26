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

package org.springframework.boot.autoconfigure.nats;

import io.nats.client.Options;

import static org.junit.Assert.assertEquals;

import java.net.URI;

import org.junit.Test;

public class PropertiesTests {
    @Test
    public void testServerProperty() {
        String server = "nats://alphabet:4222";
        NatsProperties props = new NatsProperties();

        props.setServer(server);

        Options options = props.toOptions();
        URI[] servers = options.getServers().toArray(new URI[0]);
        assertEquals(1, servers.length);
        assertEquals(server, servers[0].toString());
    }

    @Test
    public void testCommaListOfServers() {
        String server1 = "nats://alphabet:4222";
        String server2 = "nats://tebahpla:4222";
        NatsProperties props = new NatsProperties();

        props.setServer(server1 + "," + server2);

        Options options = props.toOptions();
        URI[] servers = options.getServers().toArray(new URI[0]);
        assertEquals(2, servers.length);
        assertEquals(server1, servers[0].toString());
        assertEquals(server2, servers[1].toString());
    }

    @Test
    public void testFluentProperties() {
        String server = "nats://alphabet:4222";
        NatsProperties props = new NatsProperties();

        Options options = props.server(server).toOptions();
        URI[] servers = options.getServers().toArray(new URI[0]);
        assertEquals(1, servers.length);
        assertEquals(server, servers[0].toString());
    }
}