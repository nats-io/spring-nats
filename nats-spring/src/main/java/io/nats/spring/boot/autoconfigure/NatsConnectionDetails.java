package io.nats.spring.boot.autoconfigure;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
import org.springframework.lang.Nullable;

/**
 * Details required to make connection to Nats Server.
 *
 * @author Kunal Varpe
 */
public interface NatsConnectionDetails extends ConnectionDetails {

    /**
     * Nats server url
     *
     * @return the nats server url
     */
    @Nullable
    default String getServer() {
        return null;
    }

}
