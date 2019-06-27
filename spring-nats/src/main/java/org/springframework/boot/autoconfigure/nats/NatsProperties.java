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

import java.time.Duration;

import io.nats.client.Nats;
import io.nats.client.Options;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConditionalOnClass({ Options.class })
@ConfigurationProperties(prefix = "spring.nats")
public class NatsProperties {

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

	public NatsProperties() {
	}

	public String getServer() {
		return this.server;
	}

	public void setServer(String server) {
		this.server = server;
	}

	public NatsProperties server(String server) {
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

	public NatsProperties connectionName(String connectionName) {
		this.connectionName = connectionName;
		return this;
	}

	public NatsProperties maxReconnect(int maxReconnect) {
		this.maxReconnect = maxReconnect;
		return this;
	}

	public NatsProperties reconnectWait(Duration reconnectWait) {
		this.reconnectWait = reconnectWait;
		return this;
	}

	public NatsProperties connectionTimeout(Duration connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
		return this;
	}

	public NatsProperties pingInterval(Duration pingInterval) {
		this.pingInterval = pingInterval;
		return this;
	}

	public NatsProperties reconnectBufferSize(long reconnectBufferSize) {
		this.reconnectBufferSize = reconnectBufferSize;
		return this;
	}

	public NatsProperties username(String username) {
		this.username = username;
		return this;
	}

	public NatsProperties password(String password) {
		this.password = password;
		return this;
	}

	public NatsProperties token(String token) {
		this.token = token;
		return this;
	}

	public NatsProperties inboxPrefix(String inboxPrefix) {
		this.inboxPrefix = inboxPrefix;
		return this;
	}

	public NatsProperties noEcho(boolean noEcho) {
		this.noEcho = noEcho;
		return this;
	}

	public NatsProperties utf8Support(boolean utf8Support) {
		this.utf8Support = utf8Support;
		return this;
	}

	public NatsProperties credentials(String credentials) {
		this.credentials = credentials;
		return this;
	}

	public Options toOptions() {
		return toOptionsBuilder().build();
	}

	public Options.Builder toOptionsBuilder() {
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
