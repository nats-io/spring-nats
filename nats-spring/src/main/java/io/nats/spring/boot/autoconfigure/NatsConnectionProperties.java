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

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

import io.nats.client.Nats;
import io.nats.client.Options;

@ConditionalOnClass({ Options.class })
public class NatsConnectionProperties {
	/**
	 * URL for the nats server, can be a comma separated list.
	 */
	private String server;

	/**
	 * Connection name, shows up in thread names.
	 */
	private String connectionName;

	/**
	 * Maximum reconnect attempts if a connection is lost, after the initial
	 * connection.
	 */
	private int maxReconnect = Options.DEFAULT_MAX_RECONNECT;

	/**
	 * Time to wait between reconnect attempts to the same server url.
	 */
	private Duration reconnectWait = Options.DEFAULT_RECONNECT_WAIT;

	/**
	 * Timeout for the initial connection, if this time is passed, the connection
	 * will fail and no reconnect attempts are made.
	 */
	private Duration connectionTimeout = Options.DEFAULT_CONNECTION_TIMEOUT;

	/**
	 * Time between pings to the server to check "liveness".
	 */
	private Duration pingInterval = Options.DEFAULT_PING_INTERVAL;

	/**
	 * Size of the buffer, in bytes, used to hold outgoing messages during
	 * reconnect.
	 */
	private long reconnectBufferSize = Options.DEFAULT_RECONNECT_BUF_SIZE;

	/**
	 * Prefix to use for inboxes, generally the default is used but custom prefixes
	 * can allow security controls.
	 */
	private String inboxPrefix = Options.DEFAULT_INBOX_PREFIX;

	/**
	 * Default KeyStore format/type for KeyManager instances
	 */
	private final String defaultKeyStoreType = "PKCS12";

	/**
	 * Default KeyStore format/type for TrustManager instances
	 */
	private final String defaultTrustStoreType = "PKCS12";

	/**
	 * Default KeyStore Provider Algorithm for KeyManager instances
	 */
	private final String defaultKeyStoreProviderAlgorithm = "SunX509";

	/**
	 * Default KeyStore Provider Algorithm for TrustManager instances
	 */
	private final String defaultTrustStoreProviderAlgorithm = "SunX509";

	/**
	 * Whether or not the server will send messages sent from this connection back
	 * to the connection.
	 */
	private boolean noEcho;

	/**
	 * Whether or not to treat subjects as UTF-8, the default is ASCII.
	 */
	private boolean utf8Support;

	/**
	 * Authentication user name. Requires the password, but not the token, or
	 * credentials, or NKey.
	 */
	private String username;

	/**
	 * Authentication password. Requires the username, but not the token, or
	 * credentials, or NKey.
	 */
	private String password;

	/**
	 * Authentication token, do not use with username/password, or credentials, or
	 * NKey.
	 */
	private String token;

	/**
	 * User credentials file path, do not use with user/password, or token, or NKey.
	 * Credentials are used by account enabled servers.
	 */
	private String credentials;

	/**
	 * Private key (seed) for NKey authentication, do not use with user/password, or
	 * token, or credentials.
	 */
	private String nkey;

	/**
	 * Path to the SSL keystore.
	 */
	private String keyStorePath;

	/**
	 * Password for the SSL keystore.
	 */
	private char[] keyStorePassword;

	/**
	 * Type of SSL keystore, generally the default is used.
	 */
	private String keyStoreType;

	/**
	 * Path the the SSL trust store, for verifying the server.
	 */
	private String trustStorePath;

	/**
	 * Password for the SSL trust store used to verify the server.
	 */
	private char[] trustStorePassword;

	/**
	 * Provider Algorithm for the the SSL key store, for verifying the server.
	 */
	private String keyStoreProvider;

	/**
	 * Provider Algorithm for the SSL trust store used to verify the server.
	 */
	private String trustStoreProvider;

	/**
	 * TLS Protocol version for the SSL Context Values: TLSv1.2, TLSv1.3
	 */
	private String tlsProtocol;

	/**
	 * Type of SSL trust store, generally the default is used.
	 */
	private String trustStoreType;

	public NatsConnectionProperties() {
	}

	/**
	 * @return url for the nats server
	 */
	public String getServer() {
		return this.server;
	}

	/**
	 * @param server url for the nats server
	 */
	public void setServer(String server) {
		this.server = server;
	}

	/**
	 * @return a name used for the connection
	 */
	public String getConnectionName() {
		return this.connectionName;
	}

	/**
	 * @param connectionName a name to associate with the connection
	 */
	public void setConnectionName(String connectionName) {
		this.connectionName = connectionName;
	}

	/**
	 * @return maximum times to try to reconnect
	 */
	public int getMaxReconnect() {
		return this.maxReconnect;
	}

	/**
	 * @param maxReconnect maximum times to try to reconnect
	 */
	public void setMaxReconnect(int maxReconnect) {
		this.maxReconnect = maxReconnect;
	}

	/**
	 * @return time to wait between reconnect attempts on the same server url
	 */
	public Duration getReconnectWait() {
		return this.reconnectWait;
	}

	/**
	 * @param reconnectWait time to wait between reconnect attempts on the same
	 *                      server url
	 */
	public void setReconnectWait(Duration reconnectWait) {
		this.reconnectWait = reconnectWait;
	}

	/**
	 * @return maximum time for initial connection
	 */
	public Duration getConnectionTimeout() {
		return this.connectionTimeout;
	}

	/**
	 * @param connectionTimeout maximum time for initial connection
	 */
	public void setConnectionTimeout(Duration connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}

	/**
	 * @return time between server pings
	 */
	public Duration getPingInterval() {
		return this.pingInterval;
	}

	/**
	 * @param pingInterval time between server pings
	 */
	public void setPingInterval(Duration pingInterval) {
		this.pingInterval = pingInterval;
	}

	/**
	 * @return size of the buffer, in bytes, used to store publish messages during
	 *         reconnect
	 */
	public long getReconnectBufferSize() {
		return this.reconnectBufferSize;
	}

	/**
	 * @param reconnectBufferSize size of the buffer, in bytes, used to store
	 *                            publish messages during reconnect
	 */
	public void setReconnectBufferSize(long reconnectBufferSize) {
		this.reconnectBufferSize = reconnectBufferSize;
	}

	/**
	 * @return username to use with password for authenticaiton
	 */
	public String getUsername() {
		return this.username;
	}

	/**
	 * @param username to use with password for authenticaiton
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * @return password to use with username for authenticaiton
	 */
	public String getPassword() {
		return this.password;
	}

	/**
	 * @param password to use with username for authenticaiton
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * @return authentication token to use with the server
	 */
	public String getToken() {
		return this.token;
	}

	/**
	 * @param token authentication token to use with the server
	 */
	public void setToken(String token) {
		this.token = token;
	}

	/**
	 * @return private key (seed) for NKey authentication with the server
	 */
	public String getNkey() {
		return nkey;
	}

	/**
	 * @param nkey private key (seed) for NKey authentication with the server
	 */
	public void setNkey(String nkey) {
		this.nkey = nkey;
	}

	/**
	 * @return prefix to use for request/reply inboxes
	 */
	public String getInboxPrefix() {
		return this.inboxPrefix;
	}

	/**
	 * @param inboxPrefix custom prefix to use for request/reply inboxes
	 */
	public void setInboxPrefix(String inboxPrefix) {
		this.inboxPrefix = inboxPrefix;
	}

	/**
	 * @return whether or not to block echo messages, messages that were sent by
	 *         this connection
	 */
	public boolean isNoEcho() {
		return this.noEcho;
	}

	/**
	 * @return whether or not to block echo messages, messages that were sent by
	 *         this connection
	 */
	public boolean getNoEcho() {
		return this.noEcho;
	}

	/**
	 * @param noEcho enable or disable echo messages, messages that are sent by this
	 *               connection back to this connection
	 */
	public void setNoEcho(boolean noEcho) {
		this.noEcho = noEcho;
	}

	/**
	 * @return whether or not the client should support for UTF8 subject names
	 */
	public boolean isUtf8Support() {
		return this.utf8Support;
	}

	/**
	 * @return whether or not the client should support for UTF8 subject names
	 */
	public boolean getUtf8Support() {
		return this.utf8Support;
	}

	/**
	 * @param utf8Support whether or not the client should support for UTF8 subject
	 *                    names
	 */
	public void setUtf8Support(boolean utf8Support) {
		this.utf8Support = utf8Support;
	}

	/**
	 * @return path to the credentials file to use for authentication with an
	 *         account enabled server
	 */
	public String getCredentials() {
		return this.credentials;
	}

	/**
	 * @param credentials path to the credentials file to use for authentication
	 *                    with an account enabled server
	 */
	public void setCredentials(String credentials) {
		this.credentials = credentials;
	}

	/**
	 * @return path to the SSL Keystore
	 */
	public String getKeyStorePath() {
		return this.keyStorePath;
	}

	/**
	 * @param keyStorePath file path for the SSL Keystore
	 */
	public void setKeyStorePath(String keyStorePath) {
		this.keyStorePath = keyStorePath;
	}

	/**
	 * @return password used to unlock the keystore
	 */
	public char[] getKeyStorePassword() {
		return this.keyStorePassword;
	}

	/**
	 * @param keyStorePassword used to unlock the keystore
	 */
	public void setKeyStorePassword(char[] keyStorePassword) {
		this.keyStorePassword = keyStorePassword;
	}

	/**
	 * @return type of keystore to use for SSL connections
	 */
	public String getKeyStoreType() {
		return this.keyStoreType;
	}

	/**
	 * @param keyStoreType generally the default, but available for special keystore
	 *                     formats/types
	 */
	public void setKeyStoreType(String keyStoreType) {
		this.keyStoreType = keyStoreType;
	}

	/**
	 * @return file path for the SSL trust store
	 */
	public String getTrustStorePath() {
		return this.trustStorePath;
	}

	/**
	 * @param trustStorePath file path for the SSL trust store
	 */
	public void setTrustStorePath(String trustStorePath) {
		this.trustStorePath = trustStorePath;
	}

	/**
	 * @return password used to unlock the trust store
	 */
	public char[] getTrustStorePassword() {
		return this.trustStorePassword;
	}

	/**
	 * @param trustStorePassword used to unlock the trust store
	 */
	public void setTrustStorePassword(char[] trustStorePassword) {
		this.trustStorePassword = trustStorePassword;
	}

	/**
	 * @return type of keystore to use for SSL connections
	 */
	public String getTrustStoreType() {
		return this.trustStoreType;
	}

	/**
	 * @param trustStoreType generally the default, but available for special trust
	 *                       store formats/types
	 */
	public void setTrustStoreType(String trustStoreType) {
		this.trustStoreType = trustStoreType;
	}

	/**
	 * @return keyStoreProvider of keystore to use for SSL connections
	 */
	public String getKeyStoreProvider() {
		return keyStoreProvider;
	}

	/**
	 * @param keyStoreProvider defaults to SunX509. Alternatives include PKIX.
	 */
	public void setKeyStoreProvider(String keyStoreProvider) {
		this.keyStoreProvider = keyStoreProvider;
	}

	/**
	 * @return trustStoreProvider of keystore to use for SSL connections
	 */
	public String getTrustStoreProvider() {
		return trustStoreProvider;
	}

	/**
	 * @param trustStoreProvider defaults to SunX509. Alternatives include PKIX.
	 */
	public void setTrustStoreProvider(String trustStoreProvider) {
		this.trustStoreProvider = trustStoreProvider;
	}

	/**
	 * 
	 * @return tlsProtocol to be used in TLS handshake
	 */
	public String getTlsProtocol() {
		return tlsProtocol;
	}

	/**
	 * @param trustStoreProvider defaults to TLSv1.2
	 */
	public void setTlsProtocol(String tlsProtocol) {
		this.tlsProtocol = tlsProtocol;
	}

	/**
	 * @param serverURL used for the underlying nats connection, can be a comma
	 *                  separated list
	 * @return chainable properties
	 */
	public NatsConnectionProperties server(String serverURL) {
		this.server = serverURL;
		return this;
	}

	/**
	 * @param connectionName used for the underlying nats connection
	 * @return chainable properties
	 */
	public NatsConnectionProperties connectionName(String connectionName) {
		this.connectionName = connectionName;
		return this;
	}

	/**
	 * @param maxReconnect limit on the number of reconnect attempts to make, per
	 *                     disconnect
	 * @return chainable properties
	 */
	public NatsConnectionProperties maxReconnect(int maxReconnect) {
		this.maxReconnect = maxReconnect;
		return this;
	}

	/**
	 * @param reconnectWait time to wait between reconnect attempts to the same
	 *                      server url
	 * @return chainable properties
	 */
	public NatsConnectionProperties reconnectWait(Duration reconnectWait) {
		this.reconnectWait = reconnectWait;
		return this;
	}

	/**
	 * @param connectionTimeout maximum time to allow the initial connection to take
	 * @return chainable properties
	 */
	public NatsConnectionProperties connectionTimeout(Duration connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
		return this;
	}

	/**
	 * @param pingInterval time between heartbeat pings to the server
	 * @return chainable properties
	 */
	public NatsConnectionProperties pingInterval(Duration pingInterval) {
		this.pingInterval = pingInterval;
		return this;
	}

	/**
	 * @param reconnectBufferSize size, in bytes, of the buffer used to store
	 *                            outgoing messages during reconnect
	 * @return chainable properties
	 */
	public NatsConnectionProperties reconnectBufferSize(long reconnectBufferSize) {
		this.reconnectBufferSize = reconnectBufferSize;
		return this;
	}

	/**
	 * @param username for authentication
	 * @return chainable properties
	 */
	public NatsConnectionProperties username(String username) {
		this.username = username;
		return this;
	}

	/**
	 * @param password for authentication
	 * @return chainable properties
	 */
	public NatsConnectionProperties password(String password) {
		this.password = password;
		return this;
	}

	/**
	 * @param token for authentication
	 * @return chainable properties
	 */
	public NatsConnectionProperties token(String token) {
		this.token = token;
		return this;
	}

	/**
	 * @param nkey private key (seed) for NKey authentication
	 * @return chainable properties
	 */
	public NatsConnectionProperties nkey(String nkey) {
		this.nkey = nkey;
		return this;
	}

	/**
	 * @param inboxPrefix custom prefix to use for request/reply inboxes
	 * @return chainable properties
	 */
	public NatsConnectionProperties inboxPrefix(String inboxPrefix) {
		this.inboxPrefix = inboxPrefix;
		return this;
	}

	/**
	 * @param noEcho whether or not to send messages published by this connection
	 *               back to it's subscribers
	 * @return chainable properties
	 */
	public NatsConnectionProperties noEcho(boolean noEcho) {
		this.noEcho = noEcho;
		return this;
	}

	/**
	 * @param utf8Support whether or not to support utf8 subject names
	 * @return chainable properties
	 */
	public NatsConnectionProperties utf8Support(boolean utf8Support) {
		this.utf8Support = utf8Support;
		return this;
	}

	/**
	 * @param credentials file path to the user credentials to use for
	 *                    authentication
	 * @return chainable properties
	 */
	public NatsConnectionProperties credentials(String credentials) {
		this.credentials = credentials;
		return this;
	}

	/**
	 * @param keyStorePath file path to SSL Key Store
	 * @return chainable properties
	 */
	public NatsConnectionProperties keyStorePath(String keyStorePath) {
		this.keyStorePath = keyStorePath;
		return this;
	}

	/**
	 * @param keyStorePassword required to unlock the SSL Key Store
	 * @return chainable properties
	 */
	public NatsConnectionProperties keyStorePassword(char[] keyStorePassword) {
		this.keyStorePassword = keyStorePassword;
		return this;
	}

	/**
	 * @param trustStorePath file path to SSL Trust Store
	 * @return chainable properties
	 */
	public NatsConnectionProperties trustStorePath(String trustStorePath) {
		this.trustStorePath = trustStorePath;
		return this;
	}

	/**
	 * @param trustStorePassword required to unlock the SSL Trust Store
	 * @return chainable properties
	 */
	public NatsConnectionProperties trustStorePassword(char[] trustStorePassword) {
		setTrustStorePassword(trustStorePassword);
		return this;
	}

	/**
	 * @param keyStoreType type/format of the SSL Key Store
	 * @return chainable properties
	 */
	public NatsConnectionProperties keyStoreType(String keyStoreType) {
		this.keyStoreType = keyStoreType;
		return this;
	}

	/**
	 * @param keyStoreProvider of the SSL Key Store
	 * @return chainable properties
	 */
	public NatsConnectionProperties keyStoreProvider(String keyStoreProvider) {
		this.keyStoreProvider = keyStoreProvider;
		return this;
	}

	/**
	 * @param trustStoreType type/format of the SSL Trust Store
	 * @return chainable properties
	 */
	public NatsConnectionProperties trustStoreType(String trustStoreType) {
		this.trustStoreType = trustStoreType;
		return this;
	}

	/**
	 * @param trustStoreProvider of the SSL Trust Store
	 * @return chainable properties
	 */
	public NatsConnectionProperties trustStoreProvider(String trustStoreProvider) {
		this.trustStoreProvider = trustStoreProvider;
		return this;
	}

	/**
	 * @param trustStoreProvider of the SSL Trust Store
	 * @return chainable properties
	 */
	public NatsConnectionProperties tlsProtocol(String tlsProtocol) {
		this.tlsProtocol = tlsProtocol;
		return this;
	}

	protected KeyStore loadKeystore(String path, char[] password, String keyStoreType)
			throws IOException, GeneralSecurityException {
		KeyStore store = KeyStore.getInstance(keyStoreType);

		try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(path))) {
			store.load(in, password);
		}

		return store;
	}

	protected KeyManager[] createKeyManagers(String path, char[] password, String keyStoreProvider, String keyStoreType)
			throws IOException, GeneralSecurityException {
		if (keyStoreProvider == null || keyStoreProvider.length() == 0) {
			keyStoreProvider = defaultKeyStoreProviderAlgorithm;
		}

		if (keyStoreType == null || keyStoreType.length() == 0) {
			keyStoreType = defaultKeyStoreType;
		}

		if (password == null || password.length == 0) {
			password = new char[0];
		}

		KeyStore store = this.loadKeystore(path, password, keyStoreType);
		KeyManagerFactory factory = KeyManagerFactory.getInstance(keyStoreProvider);
		factory.init(store, password);
		return factory.getKeyManagers();
	}

	protected TrustManager[] createTrustManagers(String path, char[] password, String trustStoreProvider,
			String trustStoreType) throws IOException, GeneralSecurityException {
		if (trustStoreProvider == null || trustStoreProvider.length() == 0) {
			trustStoreProvider = defaultTrustStoreProviderAlgorithm;
		}

		if (trustStoreType == null || trustStoreType.length() == 0) {
			trustStoreType = defaultTrustStoreType;
		}

		if (password == null || password.length == 0) {
			password = new char[0];
		}

		KeyStore store = loadKeystore(path, password, trustStoreType);
		TrustManagerFactory factory = TrustManagerFactory.getInstance(trustStoreProvider);
		
		/* 
		 * Trust Store doesn't need to load any keys from the store (just certs)
		 * so passing a password is not necessary
		 */
		factory.init(store); 
		
		return factory.getTrustManagers();
	}

	/**
	 * Create a SSLContext with the given tlsProtocol
	 * 
	 * @return SSLContext
	 * @throws IOException
	 * @throws GeneralSecurityException
	 */
	protected SSLContext createSSLContext() throws IOException, GeneralSecurityException {
		
		if (this.tlsProtocol == null || this.tlsProtocol.length() == 0) {
			this.tlsProtocol = Options.DEFAULT_SSL_PROTOCOL;
		}

		SSLContext ctx = SSLContext.getInstance(this.tlsProtocol);
		ctx.init(
				this.createKeyManagers(this.keyStorePath, this.keyStorePassword, this.keyStoreProvider,
						this.keyStoreType),
				this.createTrustManagers(this.trustStorePath, this.trustStorePassword, this.trustStoreProvider,
						this.trustStoreType),
				new SecureRandom());
		return ctx;
	}

	/**
	 * @return NATS options based on this set of properties
	 * @throws IOException              if there is a problem reading a file or
	 *                                  setting up the SSL context
	 * @throws GeneralSecurityException if there is a problem setting up the SSL
	 *                                  context
	 */
	public Options toOptions() throws IOException, GeneralSecurityException {
		return toOptionsBuilder().build();
	}

	/**
	 * @return NATS options builder based on this set of properties, useful if other
	 *         settings are required before connect is called
	 * @throws IOException              if there is a problem reading a file or
	 *                                  setting up the SSL context
	 * @throws GeneralSecurityException if there is a problem setting up the SSL
	 *                                  context
	 */
	public Options.Builder toOptionsBuilder() throws IOException, GeneralSecurityException {
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

		if (this.nkey != null && this.nkey.length() > 0) {
			builder = builder.authHandler(Nats.staticCredentials(null, this.nkey.toCharArray()));
		} else if (this.credentials != null && this.credentials.length() > 0) {
			builder = builder.authHandler(Nats.credentials(this.credentials));
		} else if (this.token != null && this.token.length() > 0) {
			builder = builder.token(this.token.toCharArray());
		} else if (this.username != null && this.username.length() > 0) {
			builder = builder.userInfo(this.username.toCharArray(), this.password.toCharArray());
		}

		if (this.keyStorePath != null && this.keyStorePath.length() > 0 && this.trustStorePath != null
				&& this.trustStorePath.length() > 0) {
			builder.sslContext(this.createSSLContext());
		}

		return builder;
	}

	@Override
	public String toString() {
		return "{" + " server='" + getServer() + "'," + " name='" + getConnectionName() + "'," + " maxReconnect='"
				+ getMaxReconnect() + "'," + " reconnectWait='" + getReconnectWait() + "'," + " connectionTimeout='"
				+ getConnectionTimeout() + "'," + " pingInterval='" + getPingInterval() + "',"
				+ " reconnectBufferSize='" + getReconnectBufferSize() + "'," + " noEcho='" + getNoEcho() + "',"
				+ " utf8='" + getUtf8Support() + "'," + " user='" + getUsername() + "'," + " password='" + getPassword()
				+ "'," + " token='" + getToken() + "'," + " creds='" + getCredentials() + "'," + " nkey='" + getNkey()
				+ "'," + "}";
	}

}
