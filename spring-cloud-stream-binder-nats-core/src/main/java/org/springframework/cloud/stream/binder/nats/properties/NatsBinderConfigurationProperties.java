/*
 * Copyright 2015-2018 the original author or authors.
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

package org.springframework.cloud.stream.binder.nats.properties;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author The NATS Authors
 */
@ConfigurationProperties(prefix = "spring.cloud.stream.nats.binder")
public class NatsBinderConfigurationProperties {

	private String connectionName;

	private Duration connectionTimeout;
	private String inboxPrefix;
	private int maxControlLine;
	private int maxPingsOut;
	private int maxReconnect;
	private int reconnectWait;

	/* TODO:  Make props for these...
	bufferSize(int size)
	Sets the initial size for buffers in the connection, primarily for testing.

	Set the ConnectionListener to receive asynchronous notifications of disconnect events.
	Options.Builder	connectionName(java.lang.String name)
	Set the connection's optional Name.
	Options.Builder	connectionTimeout(java.time.Duration time)
	Set the timeout for connection attempts.
	Options.Builder	dataPortType(java.lang.String dataPortClassName)
	The class to use for this connections data port.
	Options.Builder	errorListener(ErrorListener listener)
	Set the ErrorListener to receive asynchronous error events related to this connection.
	Options.Builder	executor(java.util.concurrent.ExecutorService executor)
	Set the ExecutorService used to run threaded tasks.
	Options.Builder	inboxPrefix(java.lang.String prefix)
	Set the connection's inbox prefix.
	Options.Builder	maxControlLine(int bytes)
	Set the maximum length of a control line sent by this connection.
	Options.Builder	maxPingsOut(int max)
	Set the maximum number of pings the client can have in flight.
	Options.Builder	maxReconnects(int max)
	Set the maximum number of reconnect attempts.
	Options.Builder	noEcho()
	Turn off echo.
	Options.Builder	noRandomize()
	Turn off server pool randomization.
	Options.Builder	noReconnect()
	Equivalent to calling maxReconnects with 0, maxReconnects.
	Options.Builder	oldRequestStyle()
	Turn on the old request style that uses a new inbox and subscriber for each request.
	Options.Builder	opentls()
	Set the SSL context to one that accepts any server certificate and has no client certificates.
	Options.Builder	pedantic()
	Turn on pedantic mode for the server, in relation to this connection.
	Options.Builder	pingInterval(java.time.Duration time)
	Set the interval between attempts to pings the server.
	Options.Builder	reconnectBufferSize(long size)
	Set the maximum number of bytes to buffer in the client when trying to reconnect.
	Options.Builder	reconnectWait(java.time.Duration time)
	Set the time to wait between reconnect attempts to the same server.
	Options.Builder	requestCleanupInterval(java.time.Duration time)
	Set the interval between cleaning passes on outstanding request futures that are cancelled or timeout in the application code.
	Options.Builder	secure()
	Sets the options to use the default SSL Context, if it exists.
	Options.Builder	server(java.lang.String serverURL)
	Add a server to the list of known servers.
	Options.Builder	servers(java.lang.String[] servers)
	Add an array of servers to the list of known servers.
	Options.Builder	sslContext(javax.net.ssl.SSLContext ctx)
	Set the SSL context, requires that the server supports TLS connections and the URI specifies TLS.
	Options.Builder	supportUTF8Subjects()
	The client protocol is not clear about the encoding for subject names.
	Options.Builder	token(java.lang.String token)
	Set the token for token-based authentication.
	Options.Builder	turnOnAdvancedStats()
	Turn on advanced stats, primarily for test/benchmarks.
	Options.Builder	userInfo(java.lang.String userName, java.lang.String password)
	Set the username and password for basic authentication.
	Options.Builder	verbose()
	Turn on verbose mode with the server.
	*/

	/**
	 * Cluster member node names; only needed for queue affinity.
	 */
	private String[] servers = new String[0];

	/**
	 * Prefix for connection names from this binder.
	 */
	private String connectionNamePrefix;

	public String[] getServers() {
		return servers;
	}

	public void setServers(String[] nodes) {
		this.servers = servers;
	}
}
