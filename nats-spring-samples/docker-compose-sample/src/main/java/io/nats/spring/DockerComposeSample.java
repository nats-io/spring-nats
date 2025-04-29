package io.nats.spring;

import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.nio.charset.StandardCharsets;

@SpringBootApplication
public class DockerComposeSample {
    public static void main(String[] args) {
        SpringApplication.run(DockerComposeSample.class);
    }

    private static final Log logger = LogFactory.getLog(DockerComposeSample.class);
    private Dispatcher dispatcher;

    @Bean
    ApplicationRunner runner(Connection nc) {
        return args -> {
            logger.info("starting autoconfigure listener with connection " + nc);

            this.dispatcher = nc.createDispatcher(m -> {
                logger.info("received message on " + m.getSubject() + " with reply to " + m.getReplyTo());
                if (m.getReplyTo() != null) {
                    nc.publish(m.getReplyTo(), m.getData());
                }
            });

            String subject = "dataIn";

            if (args.getSourceArgs().length > 0) {
                subject = args.getSourceArgs()[0];
            }

            logger.info("subscribing to " + subject);
            this.dispatcher.subscribe(subject);
        };
    }
}
