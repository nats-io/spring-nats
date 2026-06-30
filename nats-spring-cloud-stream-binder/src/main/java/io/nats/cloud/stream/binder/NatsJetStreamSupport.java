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

package io.nats.cloud.stream.binder;

import io.nats.client.Connection;
import io.nats.client.JetStreamApiException;
import io.nats.client.JetStreamManagement;
import io.nats.client.api.StorageType;
import io.nats.client.api.StreamConfiguration;
import io.nats.client.api.StreamInfo;
import io.nats.cloud.stream.binder.properties.NatsProducerProperties;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

class NatsJetStreamSupport {
    static final Duration DEFAULT_JETSTREAM_POLL_TIMEOUT = Duration.ofSeconds(1);

    private static final int HTTP_NOT_FOUND = 404;
    private static final int STREAM_NOT_FOUND = 10059;

    private NatsJetStreamSupport() {
    }

    static String normalize(String value) {
        if (!hasText(value)) {
            return null;
        }

        return value.trim();
    }

    static boolean hasText(String value) {
        return value != null && value.trim().length() > 0;
    }

    static void provisionStream(Connection connection, String subject, NatsProducerProperties properties) {
        if (properties == null || !properties.isJetStream() || !properties.isProvisionStream()) {
            return;
        }

        provisionStream(connection,
                subject,
                properties.getStreamName(),
                properties.getStreamStorageType(),
                properties.getStreamReplicas());
    }

    private static void provisionStream(Connection connection,
                                        String subject,
                                        String streamName,
                                        StorageType storageType,
                                        Integer streamReplicas) {
        String stream = normalize(streamName);
        String streamSubject = normalize(subject);
        if (!hasText(stream)) {
            throw new IllegalStateException("NATS JetStream stream provisioning requires stream-name");
        }
        if (!hasText(streamSubject)) {
            throw new IllegalStateException("NATS JetStream stream provisioning requires a subject");
        }
        if (connection == null) {
            throw new IllegalStateException("NATS JetStream stream provisioning requires a NATS connection");
        }

        try {
            JetStreamManagement management = connection.jetStreamManagement();
            StreamInfo streamInfo = streamInfoOrNull(management, stream);
            if (streamInfo == null) {
                addStream(management, stream, streamSubject, storageType, streamReplicas);
                return;
            }

            ensureStreamContainsSubject(stream, streamInfo.getConfiguration(), streamSubject);
        } catch (IOException | JetStreamApiException | IllegalArgumentException exp) {
            throw new IllegalStateException("Failed to provision NATS JetStream stream " + stream, exp);
        }
    }

    private static void addStream(JetStreamManagement management,
                                  String stream,
                                  String subject,
                                  StorageType storageType,
                                  Integer streamReplicas)
            throws IOException, JetStreamApiException {
        try {
            management.addStream(newStreamConfiguration(stream, subject, storageType, streamReplicas));
        } catch (JetStreamApiException exp) {
            StreamInfo streamInfo = streamInfoOrNull(management, stream);
            if (streamInfo == null) {
                throw exp;
            }
            ensureStreamContainsSubject(stream, streamInfo.getConfiguration(), subject);
        }
    }

    private static StreamConfiguration newStreamConfiguration(String stream,
                                                              String subject,
                                                              StorageType storageType,
                                                              Integer streamReplicas) {
        StreamConfiguration.Builder builder = StreamConfiguration.builder()
                .name(stream)
                .subjects(subject);
        if (storageType != null) {
            builder.storageType(storageType);
        }
        if (streamReplicas != null) {
            builder.replicas(streamReplicas);
        }
        return builder.build();
    }

    private static StreamInfo streamInfoOrNull(JetStreamManagement management, String stream)
            throws IOException, JetStreamApiException {
        try {
            return management.getStreamInfo(stream);
        } catch (JetStreamApiException exp) {
            if (exp.getErrorCode() == HTTP_NOT_FOUND || exp.getApiErrorCode() == STREAM_NOT_FOUND) {
                return null;
            }
            throw exp;
        }
    }

    private static boolean hasSubject(StreamConfiguration configuration, String subject) {
        List<String> subjects = configuration.getSubjects();
        if (subjects == null || subjects.isEmpty()) {
            return false;
        }

        for (String configuredSubject : subjects) {
            if (subjectMatches(configuredSubject, subject)) {
                return true;
            }
        }

        return false;
    }

    private static void ensureStreamContainsSubject(String stream, StreamConfiguration configuration, String subject) {
        if (!hasSubject(configuration, subject)) {
            throw new IllegalStateException("NATS JetStream stream " + stream
                    + " does not include subject " + subject);
        }
    }

    private static boolean subjectMatches(String pattern, String subject) {
        String normalizedPattern = normalize(pattern);
        if (!hasText(normalizedPattern)) {
            return false;
        }

        String[] patternTokens = normalizedPattern.split("\\.");
        String[] subjectTokens = subject.split("\\.");
        for (int index = 0; index < patternTokens.length; index++) {
            String token = patternTokens[index];
            if (">".equals(token)) {
                return index < subjectTokens.length;
            }
            if (index >= subjectTokens.length) {
                return false;
            }
            if (!"*".equals(token) && !token.equals(subjectTokens[index])) {
                return false;
            }
        }

        return patternTokens.length == subjectTokens.length;
    }
}
