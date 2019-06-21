package org.springframework.boot.autoconfigure.nats;

import java.lang.InterruptedException;
import java.io.IOException;

import io.nats.client.Connection;
import io.nats.client.Nats;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for NATS.
 */
//@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ Connection.class })
@EnableConfigurationProperties(NatsProperties.class)
public class NatsAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public Connection natsConnection(NatsProperties properties) throws IOException, InterruptedException {
		return Nats.connect(properties.toOptions());
	}

}
