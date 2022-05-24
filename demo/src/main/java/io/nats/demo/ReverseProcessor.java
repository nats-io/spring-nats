package io.nats.demo;

import java.nio.charset.StandardCharsets;

import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Processor;
import org.springframework.messaging.handler.annotation.SendTo;

@EnableBinding(Processor.class)
public class ReverseProcessor {
    @StreamListener(Processor.INPUT)
    @SendTo(Processor.OUTPUT)
    public Object transform(Object message) {
        if (message instanceof byte[]) {
            String value = new String((byte[]) message, StandardCharsets.UTF_8);
            StringBuilder reverse = new StringBuilder();
            for (int i = value.length() - 1; i >= 0; i--) {
                reverse.append(value.charAt(i));
            }
            message = reverse.toString().toUpperCase().getBytes(StandardCharsets.UTF_8);
        } else if (message instanceof String) {
            String value = (String) message;
            StringBuilder reverse = new StringBuilder();
            for (int i = value.length() - 1; i >= 0; i--) {
                reverse.append(value.charAt(i));
            }
            message = reverse.toString().toUpperCase().getBytes(StandardCharsets.UTF_8);
        }
        return message;
    }
}
