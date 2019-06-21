package org.springframework.boot.autoconfigure.nats;

import io.nats.client.Options;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.nats")
@ConditionalOnClass({ Options.class })
public class NatsProperties {

    private String server;

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

    public Options toOptions() {
        return (new Options.Builder()).server(this.server).build();
    }
}
