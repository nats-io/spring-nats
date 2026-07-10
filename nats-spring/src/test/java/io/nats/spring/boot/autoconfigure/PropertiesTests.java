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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.time.Duration;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PropertiesTests {

    @TempDir
    private Path tempDir;

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
        props.setNoResolveHostnames(true);

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
        assertTrue(options.isNoResolveHostnames());
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
        props.setKeyStoreType("JKS");
        props.setTrustStorePassword("password".toCharArray());
        props.setTrustStorePath("src/test/resources/cacerts");
        props.setTrustStoreType("JKS");
        props.setTlsFirst(true);
        options = props.toOptions();
        assertNotNull(options.getSslContext());
        assertTrue(options.isTlsFirst());
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
    public void noNoRespondersDefaultsToFalseInOptions() throws Exception {
        NatsConnectionProperties props = new NatsConnectionProperties();
        props.setServer("nats://alphabet:4222");

        Options options = props.toOptions();

        assertFalse(props.getNoNoResponders());
        assertFalse(props.isNoNoResponders());
        assertFalse(options.isNoNoResponders());
    }

    @Test
    public void noNoRespondersSetterDisablesNoRespondersSupportInOptions() throws Exception {
        NatsConnectionProperties props = new NatsConnectionProperties();
        props.setServer("nats://alphabet:4222");
        props.setNoNoResponders(true);

        Options options = props.toOptions();

        assertTrue(props.getNoNoResponders());
        assertTrue(props.isNoNoResponders());
        assertTrue(options.isNoNoResponders());
    }

    @Test
    public void noNoRespondersFluentPropertyDisablesNoRespondersSupportInOptions() throws Exception {
        NatsConnectionProperties props = new NatsConnectionProperties()
                .server("nats://alphabet:4222")
                .noNoResponders(true);

        Options options = props.toOptions();

        assertTrue(props.getNoNoResponders());
        assertTrue(props.isNoNoResponders());
        assertTrue(options.isNoNoResponders());
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
        props = props.noResolveHostnames(true);
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
        assertTrue(options.isNoResolveHostnames());
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
        props = props.keyStoreType("JKS");
        props = props.trustStorePassword("password".toCharArray());
        props = props.trustStorePath("src/test/resources/cacerts");
        props = props.trustStoreType("JKS");
        props = props.tlsFirst(true);
        options = props.toOptions();
        assertNotNull(options.getSslContext());
        assertTrue(options.isTlsFirst());
    }

    @Test
    public void testUsernameWithEmptyPasswordIsPassedToOptions() throws Exception {
        NatsConnectionProperties props = new NatsConnectionProperties();
        props.setServer("nats://alphabet:4222");
        props.setUsername("user");
        props.setPassword("");

        Options options = props.toOptions();

        assertEquals("user", options.getUsername());
        assertEquals("", options.getPassword());
    }

    @Test
    public void testUsernameWithWhitespacePasswordIsPassedToOptions() throws Exception {
        NatsConnectionProperties props = new NatsConnectionProperties();
        props.setServer("nats://alphabet:4222");
        props.setUsername("user");
        props.setPassword(" ");

        Options options = props.toOptions();

        assertEquals("user", options.getUsername());
        assertEquals(" ", options.getPassword());
    }

    @Test
    public void testTokenOverridesUsernameWithoutPassword() throws Exception {
        NatsConnectionProperties props = new NatsConnectionProperties();
        props.setServer("nats://alphabet:4222");
        props.setUsername("user");
        props.setToken("token");

        Options options = props.toOptions();

        assertEquals("token", options.getToken());
        assertNull(options.getUsername());
        assertNull(options.getPassword());
    }

    @Test
    public void testWhitespaceTokenOverridesUserPasswordLikeOtherTokenValues() throws Exception {
        NatsConnectionProperties props = new NatsConnectionProperties();
        props.setServer("nats://alphabet:4222");
        props.setUsername("user");
        props.setPassword("pass");
        props.setToken(" ");

        Options options = props.toOptions();

        assertEquals(" ", options.getToken());
        assertNull(options.getUsername());
        assertNull(options.getPassword());
    }

    @Test
    public void testKeyStoreWithoutTrustStoreDoesNotEnableTls() throws Exception {
        NatsConnectionProperties props = new NatsConnectionProperties();
        props.setServer("nats://alphabet:4222");
        props.setKeyStorePath("src/test/resources/keystore.jks");

        Options options = props.toOptions();

        assertNull(options.getSslContext());
    }

    @Test
    public void testTrustStoreWithoutKeyStoreDoesNotEnableTls() throws Exception {
        NatsConnectionProperties props = new NatsConnectionProperties();
        props.setServer("nats://alphabet:4222");
        props.setTrustStorePath("src/test/resources/cacerts");

        Options options = props.toOptions();

        assertNull(options.getSslContext());
    }

    @Test
    public void testWhitespaceKeyStorePathWithTrustStoreIsTreatedAsConfiguredPath() {
        NatsConnectionProperties props = new NatsConnectionProperties();
        props.setServer("nats://alphabet:4222");
        props.setKeyStorePath(" ");
        props.setTrustStorePath("src/test/resources/cacerts");

        assertThatThrownBy(props::toOptions).isInstanceOf(java.io.IOException.class);
    }

    @Test
    public void testWhitespaceTrustStorePathWithKeyStoreIsTreatedAsConfiguredPath() {
        NatsConnectionProperties props = new NatsConnectionProperties();
        props.setServer("nats://alphabet:4222");
        props.setKeyStorePath("src/test/resources/keystore.jks");
        props.setTrustStorePath(" ");

        assertThatThrownBy(props::toOptions).isInstanceOf(java.io.IOException.class);
    }

    @Test
    public void testBlankAuthValuesAreIgnored() throws Exception {
        NatsConnectionProperties props = new NatsConnectionProperties();
        props.setServer("nats://alphabet:4222");
        props.setUsername("");
        props.setPassword("ignored");
        props.setToken("");
        props.setCredentials("");
        props.setNkey("");

        Options options = props.toOptions();

        assertNull(options.getUsername());
        assertNull(options.getPassword());
        assertNull(options.getToken());
        assertNull(options.getAuthHandler());
    }

    @Test
    public void testWhitespaceAuthValuesArePassedThroughLikeConfiguredValues() throws Exception {
        NatsConnectionProperties props = new NatsConnectionProperties();
        props.setServer("nats://alphabet:4222");
        props.setUsername(" ");
        props.setPassword("ignored");
        props.setToken(" ");
        props.setCredentials(" ");
        props.setNkey(" ");

        Options options = props.toOptions();

        assertNotNull(options.getAuthHandler());
        assertNull(options.getUsername());
        assertNull(options.getPassword());
        assertNull(options.getToken());
    }

    @Test
    public void testNkeyAuthUsesJwtWhenPresent() throws Exception {
        NatsConnectionProperties props = new NatsConnectionProperties();
        props.setServer("nats://alphabet:4222");
        props.setJwt("jwt");
        props.setNkey("nkey");

        Options options = props.toOptions();

        assertNull(options.getToken());
        assertNull(options.getUsername());
        assertNull(options.getPassword());
        assertNotNull(options.getAuthHandler());
        assertEquals("StringAuthHandler", options.getAuthHandler().getClass().getSimpleName());
    }

    @Test
    public void testDefaultTlsStoreSettingsUseEmptyPasswords() throws Exception {
        Path keyStore = emptyPkcs12Store("keystore.p12");
        Path trustStore = emptyPkcs12Store("truststore.p12");
        NatsConnectionProperties props = new NatsConnectionProperties();
        props.setServer("nats://alphabet:4222");
        props.setKeyStorePath(keyStore.toString());
        props.setTrustStorePath(trustStore.toString());

        Options options = props.toOptions();

        assertNotNull(options.getSslContext());
    }

    @Test
    public void testBlankTlsProviderAndTypeFallBackToDefaults() throws Exception {
        Path keyStore = emptyPkcs12Store("blank-keystore.p12");
        Path trustStore = emptyPkcs12Store("blank-truststore.p12");
        NatsConnectionProperties props = new NatsConnectionProperties();
        props.setServer("nats://alphabet:4222");
        props.setKeyStorePath(keyStore.toString());
        props.setKeyStorePassword(new char[0]);
        props.setKeyStoreProvider("");
        props.setKeyStoreType("");
        props.setTrustStorePath(trustStore.toString());
        props.setTrustStorePassword(new char[0]);
        props.setTrustStoreProvider("");
        props.setTrustStoreType("");

        Options options = props.toOptions();

        assertNotNull(options.getSslContext());
    }

    @Test
    public void testExplicitTlsProviderAndProtocolAreUsed() throws Exception {
        char[] password = "changeit".toCharArray();
        Path keyStore = pkcs12Store("explicit-keystore.p12", password);
        Path trustStore = pkcs12Store("explicit-truststore.p12", password);
        NatsConnectionProperties props = new NatsConnectionProperties();
        props.setServer("nats://alphabet:4222");
        props.setKeyStorePath(keyStore.toString());
        props.setKeyStorePassword(password);
        props.setKeyStoreProvider(KeyManagerFactory.getDefaultAlgorithm());
        props.setKeyStoreType("PKCS12");
        props.setTrustStorePath(trustStore.toString());
        props.setTrustStorePassword(password);
        props.setTrustStoreProvider(TrustManagerFactory.getDefaultAlgorithm());
        props.setTrustStoreType("PKCS12");
        props.setTlsProtocol("TLSv1.2");

        Options options = props.toOptions();

        assertNotNull(options.getSslContext());
        assertEquals("TLSv1.2", options.getSslContext().getProtocol());
    }

    @Test
    public void testBlankTlsStorePathsDoNotEnableTls() throws Exception {
        NatsConnectionProperties props = new NatsConnectionProperties();
        props.setServer("nats://alphabet:4222");
        props.setKeyStorePath("");
        props.setTrustStorePath("");

        Options options = props.toOptions();

        assertNull(options.getSslContext());
    }

    @Test
    public void testWhitespaceTlsStorePathsAreTreatedAsConfiguredPaths() {
        NatsConnectionProperties props = new NatsConnectionProperties();
        props.setServer("nats://alphabet:4222");
        props.setKeyStorePath(" ");
        props.setTrustStorePath(" ");

        assertThatThrownBy(props::toOptions).isInstanceOf(java.io.IOException.class);
    }

    @Test
    public void testSecurityAndTlsAccessors() {
        char[] keyPassword = "keypass".toCharArray();
        char[] trustPassword = "trustpass".toCharArray();
        NatsConnectionProperties props = new NatsConnectionProperties();

        props.setJwt("jwt");
        props.setInboxPrefix("_INBOX.custom");
        props.setNoResolveHostnames(true);
        props.setNoEcho(true);
        props.setUtf8Support(true);
        props.setKeyStorePath("keystore");
        props.setKeyStorePassword(keyPassword);
        props.setKeyStoreType("JKS");
        props.setKeyStoreProvider("SunX509");
        props.setTrustStorePath("truststore");
        props.setTrustStorePassword(trustPassword);
        props.setTrustStoreType("JKS");
        props.setTrustStoreProvider("SunX509");
        props.setTlsProtocol("TLSv1.2");
        props.setTlsFirst(true);

        assertEquals("jwt", props.getJwt());
        assertEquals("_INBOX.custom", props.getInboxPrefix());
        assertTrue(props.isNoResolveHostnames());
        assertTrue(props.isNoEcho());
        assertTrue(props.isUtf8Support());
        assertEquals("keystore", props.getKeyStorePath());
        assertEquals(keyPassword, props.getKeyStorePassword());
        assertEquals("JKS", props.getKeyStoreType());
        assertEquals("SunX509", props.getKeyStoreProvider());
        assertEquals("truststore", props.getTrustStorePath());
        assertEquals(trustPassword, props.getTrustStorePassword());
        assertEquals("JKS", props.getTrustStoreType());
        assertEquals("SunX509", props.getTrustStoreProvider());
        assertEquals("TLSv1.2", props.getTlsProtocol());
        assertTrue(props.isTlsFirst());
    }

    @Test
    public void testFluentSecurityAndTlsAccessors() {
        NatsConnectionProperties props = new NatsConnectionProperties()
                .jwt("jwt")
                .keyStoreProvider("SunX509")
                .trustStoreProvider("SunX509")
                .tlsProtocol("TLSv1.2");

        assertEquals("jwt", props.getJwt());
        assertEquals("SunX509", props.getKeyStoreProvider());
        assertEquals("SunX509", props.getTrustStoreProvider());
        assertEquals("TLSv1.2", props.getTlsProtocol());
    }

    @Test
    public void testToStringRedactsSecrets() {
        NatsConnectionProperties props = new NatsConnectionProperties();
        props.setServer("nats://alphabet:4222");
        props.setUsername("secret-user");
        props.setPassword("secret-pass");
        props.setToken("secret-token");
        props.setCredentials("secret-credentials");
        props.setNkey("secret-nkey");

        String text = props.toString();

        assertThat(text)
                .contains("user=**********", "password=**********", "token=**********", "creds=**********", "nkey=**********")
                .doesNotContain("secret-user", "secret-pass", "secret-token", "secret-credentials", "secret-nkey");
    }

    @Test
    public void testToStringUsesPlaceholderForMissingSecrets() {
        NatsConnectionProperties props = new NatsConnectionProperties();

        assertThat(props.toString())
                .contains("user=N/A", "password=N/A", "token=N/A", "creds=N/A", "nkey=N/A");
    }

    @Test
    public void testToStringRedactsWhitespaceSecrets() {
        NatsConnectionProperties props = new NatsConnectionProperties();
        props.setUsername(" ");
        props.setPassword(" ");
        props.setToken(" ");
        props.setCredentials(" ");
        props.setNkey(" ");

        assertThat(props.toString())
                .contains("user=**********", "password=**********", "token=**********", "creds=**********", "nkey=**********");
    }

    private Path emptyPkcs12Store(String name) throws Exception {
        return pkcs12Store(name, new char[0]);
    }

    private Path pkcs12Store(String name, char[] password) throws Exception {
        Path path = this.tempDir.resolve(name);
        KeyStore store = KeyStore.getInstance("PKCS12");
        store.load(null, password);
        try (OutputStream out = Files.newOutputStream(path)) {
            store.store(out, password);
        }
        return path;
    }
}
