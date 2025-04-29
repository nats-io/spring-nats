package io.nats.docker.compose;

import io.nats.spring.boot.autoconfigure.NatsConnectionDetails;
import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionDetailsFactory;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionSource;

/**
 * {@link DockerComposeConnectionDetailsFactory} to create {@link NatsConnectionDetails} for a {@code Nats}
 *
 * @author Kunal Varpe
 */
class NatsDockerComposeConnectionDetailsFactory extends DockerComposeConnectionDetailsFactory<NatsConnectionDetails> {
    private static final String NATS_CONTAINER_NAME = "nats";
    private static final int NAST_PORT = 4222;

    NatsDockerComposeConnectionDetailsFactory(String connectionName) {
        super(NATS_CONTAINER_NAME);
    }

    @Override
    protected NatsConnectionDetails getDockerComposeConnectionDetails(DockerComposeConnectionSource source) {
        return new NatsDockerComposeConnectionDetails(source.getRunningService());
    }

    private static final class NatsDockerComposeConnectionDetails extends DockerComposeConnectionDetails implements NatsConnectionDetails {

        private final String server;

        public NatsDockerComposeConnectionDetails(RunningService service) {
            super(service);
            this.server = "nats://%s:%s".formatted(service.host(), service.ports().get(NAST_PORT));
        }

        public String getServer() {
            return this.server;
        }
    }
}
