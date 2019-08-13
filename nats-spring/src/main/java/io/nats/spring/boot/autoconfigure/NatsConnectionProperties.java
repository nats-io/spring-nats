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

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.time.Duration;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import io.nats.client.Nats;
import io.nats.client.Options;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

@ConditionalOnClass({ Options.class })
public class NatsConnectionProperties {

	private String server;
	private String connectionName;
	private int maxReconnect = Options.DEFAULT_MAX_RECONNECT;
	private Duration reconnectWait = Options.DEFAULT_RECONNECT_WAIT;
	private Duration connectionTimeout = Options.DEFAULT_CONNECTION_TIMEOUT;
	private Duration pingInterval = Options.DEFAULT_PING_INTERVAL;
	private long reconnectBufferSize = Options.DEFAULT_RECONNECT_BUF_SIZE;
	private String inboxPrefix = Options.DEFAULT_INBOX_PREFIX;
	private boolean noEcho;
	private boolean utf8Support;
	private String username;
	private String password;
	private String token;
	private String credentials;

	private String keyStorePath;
	private char[] keyStorePassword;
	private String keyStoreType;
	private String trustStorePath;
	private char[] trustStorePassword;
	private String trustStoreType;

	public NatsConnectionProperties() {
	}

	public String getServer() {
		return this.server;
	}

	public void setServer(String server) {
		this.server = server;
	}

	public NatsConnectionProperties server(String server) {
		this.server = server;
		return this;
	}

	public String getConnectionName() {
		return this.connectionName;
	}

	public void setConnectionName(String connectionName) {
		this.connectionName = connectionName;
	}

	public int getMaxReconnect() {
		return this.maxReconnect;
	}

	public void setMaxReconnect(int maxReconnect) {
		this.maxReconnect = maxReconnect;
	}

	public Duration getReconnectWait() {
		return this.reconnectWait;
	}

	public void setReconnectWait(Duration reconnectWait) {
		this.reconnectWait = reconnectWait;
	}

	public Duration getConnectionTimeout() {
		return this.connectionTimeout;
	}

	public void setConnectionTimeout(Duration connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}

	public Duration getPingInterval() {
		return this.pingInterval;
	}

	public void setPingInterval(Duration pingInterval) {
		this.pingInterval = pingInterval;
	}

	public long getReconnectBufferSize() {
		return this.reconnectBufferSize;
	}

	public void setReconnectBufferSize(long reconnectBufferSize) {
		this.reconnectBufferSize = reconnectBufferSize;
	}

	public String getUsername() {
		return this.username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return this.password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getToken() {
		return this.token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getInboxPrefix() {
		return this.inboxPrefix;
	}

	public void setInboxPrefix(String inboxPrefix) {
		this.inboxPrefix = inboxPrefix;
	}

	public boolean isNoEcho() {
		return this.noEcho;
	}

	public boolean getNoEcho() {
		return this.noEcho;
	}

	public void setNoEcho(boolean noEcho) {
		this.noEcho = noEcho;
	}

	public boolean isUtf8Support() {
		return this.utf8Support;
	}

	public boolean getUtf8Support() {
		return this.utf8Support;
	}

	public void setUtf8Support(boolean utf8Support) {
		this.utf8Support = utf8Support;
	}

	public String getCredentials() {
		return this.credentials;
	}

	public void setCredentials(String credentials) {
		this.credentials = credentials;
	}

	public String getKeyStorePath() {
		return this.keyStorePath;
	}

	public void setKeyStorePath(String keyStorePath) {
		this.keyStorePath = keyStorePath;
	}

	public char[] getKeyStorePassword() {
		return this.keyStorePassword;
	}

	public void setKeyStorePassword(char[] keyStorePassword) {
		this.keyStorePassword = keyStorePassword;
	}

	public String getKeyStoreType() {
		return this.keyStoreType;
	}

	public void setKeyStoreType(String keyStoreType) {
		this.keyStoreType = keyStoreType;
	}

	public String getTrustStorePath() {
		return this.trustStorePath;
	}

	public void setTrustStorePath(String trustStorePath) {
		this.trustStorePath = trustStorePath;
	}

	public char[] getTrustStorePassword() {
		return this.trustStorePassword;
	}

	public void setTrustStorePassword(char[] trustStorePassword) {
		this.trustStorePassword = trustStorePassword;
	}

	public String getTrustStoreType() {
		return this.trustStoreType;
	}

	public void setTrustStoreType(String trustStoreType) {
		this.trustStoreType = trustStoreType;
	}

	public NatsConnectionProperties connectionName(String connectionName) {
		this.connectionName = connectionName;
		return this;
	}

	public NatsConnectionProperties maxReconnect(int maxReconnect) {
		this.maxReconnect = maxReconnect;
		return this;
	}

	public NatsConnectionProperties reconnectWait(Duration reconnectWait) {
		this.reconnectWait = reconnectWait;
		return this;
	}

	public NatsConnectionProperties connectionTimeout(Duration connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
		return this;
	}

	public NatsConnectionProperties pingInterval(Duration pingInterval) {
		this.pingInterval = pingInterval;
		return this;
	}

	public NatsConnectionProperties reconnectBufferSize(long reconnectBufferSize) {
		this.reconnectBufferSize = reconnectBufferSize;
		return this;
	}

	public NatsConnectionProperties username(String username) {
		this.username = username;
		return this;
	}

	public NatsConnectionProperties password(String password) {
		this.password = password;
		return this;
	}

	public NatsConnectionProperties token(String token) {
		this.token = token;
		return this;
	}

	public NatsConnectionProperties inboxPrefix(String inboxPrefix) {
		this.inboxPrefix = inboxPrefix;
		return this;
	}

	public NatsConnectionProperties noEcho(boolean noEcho) {
		this.noEcho = noEcho;
		return this;
	}

	public NatsConnectionProperties utf8Support(boolean utf8Support) {
		this.utf8Support = utf8Support;
		return this;
	}

	public NatsConnectionProperties credentials(String credentials) {
		this.credentials = credentials;
		return this;
	}

	public NatsConnectionProperties keyStorePath(String keyStorePath) {
		this.keyStorePath = keyStorePath;
		return this;
	}

	public NatsConnectionProperties keyStorePassword(char[] keyStorePassword) {
		this.keyStorePassword = keyStorePassword;
		return this;
	}

	public NatsConnectionProperties keyStoreType(String keyStoreType) {
		this.keyStoreType = keyStoreType;
		return this;
	}

	public NatsConnectionProperties trustStorePath(String trustStorePath) {
		this.trustStorePath = trustStorePath;
		return this;
	}

	public NatsConnectionProperties trustStorePassword(char[] trustStorePassword) {
		this.trustStorePassword = trustStorePassword;
		return this;
	}

	public NatsConnectionProperties trustStoreType(String trustStoreType) {
		this.trustStoreType = trustStoreType;
		return this;
	}

	protected KeyStore loadKeystore(String path, char[] password) throws IOException, GeneralSecurityException {
		KeyStore store = KeyStore.getInstance("JKS");

		try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(path))) {
			store.load(in, password);
		}

		return store;
	}

	protected KeyManager[] createKeyManagers(String path, char[] password, String type) throws IOException, GeneralSecurityException {
		if (type == null || type.length() == 0) {
			type = "SunX509";
		}

		if (password == null || password.length == 0) {
			password = new char[0];
		}

		KeyStore store = this.loadKeystore(path, password);
		KeyManagerFactory factory = KeyManagerFactory.getInstance(type);
		factory.init(store, password);
		return factory.getKeyManagers();
	}

	protected TrustManager[] createTrustManagers(String path, char[] password, String type) throws IOException, GeneralSecurityException {
		if (type == null || type.length() == 0) {
			type = "SunX509";
		}

		if (password == null || password.length == 0) {
			password = new char[0];
		}

		KeyStore store = loadKeystore(path, password);
		TrustManagerFactory factory = TrustManagerFactory.getInstance(type);
		factory.init(store);
		return factory.getTrustManagers();
	}

	protected SSLContext createSSLContext() throws IOException, GeneralSecurityException  {
		SSLContext ctx = SSLContext.getInstance(Options.DEFAULT_SSL_PROTOCOL);
		ctx.init(this.createKeyManagers(this.keyStorePath, this.keyStorePassword, this.keyStoreType),
					this.createTrustManagers(this.trustStorePath, this.trustStorePassword, this.trustStoreType), new SecureRandom());
		return ctx;
	}

	public Options toOptions()  throws IOException, GeneralSecurityException  {
		return toOptionsBuilder().build();
	}

	public Options.Builder toOptionsBuilder()  throws IOException, GeneralSecurityException  {
		Options.Builder builder = new Options.Builder();

		builder = builder.server(this.server);
		builder = builder.maxReconnects(this.maxReconnect);
		builder = builder.reconnectWait(this.reconnectWait);
		builder = builder.connectionTimeout(this.connectionTimeout);
		builder = builder.connectionName(this.connectionName);
		builder = builder.pingInterval(this.pingInterval);
		builder = builder.reconnectBufferSize(this.reconnectBufferSize);
		builder = builder.inboxPrefix(this.inboxPrefix);

		if (this.noEcho) {
			builder = builder.noEcho();
		}

		if (this.utf8Support) {
			builder = builder.supportUTF8Subjects();
		}

		if (this.credentials != null && this.credentials.length() > 0) {
			builder = builder.authHandler(Nats.credentials(this.credentials));
		}
		else if (this.token != null && this.token.length() > 0) {
			builder = builder.token(this.token);
		}
		else if (this.username != null && this.username.length() > 0) {
			builder = builder.userInfo(this.username, this.password);
		}

		if (this.keyStorePath != null && this.keyStorePath.length() > 0 &&
			this.trustStorePath != null && this.trustStorePath.length() > 0) {
			builder.sslContext(this.createSSLContext());
		}

		return builder;
	}

	@Override
	public String toString() {
		return "{" +
		" server='" + getServer() + "'," +
		" name='" + getConnectionName() + "'," +
		" maxReconnect='" + getMaxReconnect() + "'," +
		" reconnectWait='" + getReconnectWait() + "'," +
		" connectionTimeout='" + getConnectionTimeout() + "'," +
		" pingInterval='" + getPingInterval() + "'," +
		" reconnectBufferSize='" + getReconnectBufferSize() + "'," +
		" noEcho='" + getNoEcho() + "'," +
		" utf8='" + getUtf8Support() + "'," +
		" user='" + getUsername() + "'," +
		" password='" + getPassword() + "'," +
		" token='" + getToken() + "'," +
		" creds='" + getCredentials() + "'," +
			"}";
	}
}
