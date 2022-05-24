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

import io.nats.client.Options;
import org.junit.Test;

import java.net.URI;
import java.time.Duration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class PropertiesTests {

    @Test
    public void testPropertySetters() throws Exception {
        String server = "nats://alphabet:4222";
        String connectionName = "alpha";
        Duration dura = Duration.ofSeconds(7);
        long size = 100;
        NatsConnectionProperties props = new NatsConnectionProperties();

        props.setServer(server);
        props.setConnectionName(connectionName);
        props.setInboxPrefix(connectionName);
        props.setReconnectWait(dura);
        props.setConnectionTimeout(dura);
        props.setPingInterval(dura);
        props.setMaxReconnect((int) size);
        props.setReconnectBufferSize(size);

        Options options = props.toOptions();
        URI[] servers = options.getServers().toArray(new URI[0]);
        assertEquals(1, servers.length);
        assertEquals(server, servers[0].toString());
        assertEquals(connectionName, options.getConnectionName());
        assertEquals(connectionName + ".", options.getInboxPrefix());
        assertEquals(dura, options.getReconnectWait());
        assertEquals(dura, options.getConnectionTimeout());
        assertEquals(dura, options.getPingInterval());
        assertEquals(size, options.getReconnectBufferSize());
        assertEquals((int) size, options.getMaxReconnect());
        assertFalse(options.isNoEcho());
        assertFalse(options.supportUTF8Subjects());
        assertNull(options.getUsername());
        assertNull(options.getPassword());

        props.setNoEcho(true);
        props.setUtf8Support(true);
        options = props.toOptions();
        assertTrue(options.isNoEcho());
        assertTrue(options.supportUTF8Subjects());

        // Test authorization waterfall
        props.setUsername("user");
        props.setPassword("pass");
        options = props.toOptions();
        assertEquals("user", options.getUsername());
        assertEquals("pass", options.getPassword());
        assertNull(options.getAuthHandler());
        assertNull(options.getToken());

        props.setToken("token");
        options = props.toOptions();
        assertEquals("token", options.getToken());
        assertNull(options.getUsername());
        assertNull(options.getPassword());
        assertNull(options.getAuthHandler());

        props.setCredentials("credentials");
        options = props.toOptions();
        assertNull(options.getToken());
        assertNull(options.getUsername());
        assertNull(options.getPassword());
        assertNotNull(options.getAuthHandler());
        assertEquals("FileAuthHandler", options.getAuthHandler().getClass().getSimpleName());

        props.setNkey("nkey");
        options = props.toOptions();
        assertNull(options.getToken());
        assertNull(options.getUsername());
        assertNull(options.getPassword());
        assertNotNull(options.getAuthHandler());
        assertEquals("StringAuthHandler", options.getAuthHandler().getClass().getSimpleName());

        props.setKeyStorePassword("password".toCharArray());
        props.setKeyStorePath("src/test/resources/keystore.jks");
        props.setKeyStoreType("SunX509");
        props.setTrustStorePassword("password".toCharArray());
        props.setTrustStorePath("src/test/resources/cacerts");
        props.setTrustStoreType("SunX509");
        options = props.toOptions();
        assertNotNull(options.getSslContext());
    }

    @Test
    public void testCommaListOfServers() throws Exception {
        String server1 = "nats://alphabet:4222";
        String server2 = "nats://tebahpla:4222";
        NatsConnectionProperties props = new NatsConnectionProperties();

        props.setServer(server1 + "," + server2);

        Options options = props.toOptions();
        URI[] servers = options.getServers().toArray(new URI[0]);
        assertEquals(2, servers.length);
        assertEquals(server1, servers[0].toString());
        assertEquals(server2, servers[1].toString());
    }

    @Test
    public void testFluentProperties() throws Exception {
        String server = "nats://alphabet:4222";
        String connectionName = "alpha";
        Duration dura = Duration.ofSeconds(7);
        long size = 100;
        NatsConnectionProperties props = new NatsConnectionProperties();

        props = props.server(server);
        props = props.connectionName(connectionName);
        props = props.inboxPrefix(connectionName);
        props = props.reconnectWait(dura);
        props = props.connectionTimeout(dura);
        props = props.pingInterval(dura);
        props = props.maxReconnect((int) size);
        props = props.reconnectBufferSize(size);
        props = props.noEcho(true);
        props = props.utf8Support(true);

        Options options = props.toOptions();
        URI[] servers = options.getServers().toArray(new URI[0]);
        assertEquals(1, servers.length);
        assertEquals(server, servers[0].toString());
        assertEquals(connectionName, options.getConnectionName());
        assertEquals(connectionName + ".", options.getInboxPrefix());
        assertEquals(dura, options.getReconnectWait());
        assertEquals(dura, options.getConnectionTimeout());
        assertEquals(dura, options.getPingInterval());
        assertEquals(size, options.getReconnectBufferSize());
        assertEquals((int) size, options.getMaxReconnect());
        assertTrue(options.isNoEcho());
        assertTrue(options.supportUTF8Subjects());
        assertNull(options.getUsername());
        assertNull(options.getPassword());

        // Test authorization waterfall
        props = props.username("user");
        props = props.password("pass");
        options = props.toOptions();
        assertEquals("user", options.getUsername());
        assertEquals("pass", options.getPassword());
        assertNull(options.getAuthHandler());
        assertNull(options.getToken());

        props = props.token("token");
        options = props.toOptions();
        assertEquals("token", options.getToken());
        assertNull(options.getUsername());
        assertNull(options.getPassword());
        assertNull(options.getAuthHandler());

        props = props.credentials("credentials");
        options = props.toOptions();
        assertNull(options.getToken());
        assertNull(options.getUsername());
        assertNull(options.getPassword());
        assertNotNull(options.getAuthHandler());
        assertEquals("FileAuthHandler", options.getAuthHandler().getClass().getSimpleName());

        props = props.nkey("nkey");
        options = props.toOptions();
        assertNull(options.getToken());
        assertNull(options.getUsername());
        assertNull(options.getPassword());
        assertNotNull(options.getAuthHandler());
        assertEquals("StringAuthHandler", options.getAuthHandler().getClass().getSimpleName());

        props = props.keyStorePassword("password".toCharArray());
        props = props.keyStorePath("src/test/resources/keystore.jks");
        props = props.keyStoreType("SunX509");
        props = props.trustStorePassword("password".toCharArray());
        props = props.trustStorePath("src/test/resources/cacerts");
        props = props.trustStoreType("SunX509");
        options = props.toOptions();
        assertNotNull(options.getSslContext());
    }
}
