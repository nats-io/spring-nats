package org.springframework.boot.autoconfigure.nats;

import io.nats.client.Options;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.nats")
@ConditionalOnClass({ Options.class })
public class NatsProperties {
    public Options toOptions() {
        return null;
    }
}
