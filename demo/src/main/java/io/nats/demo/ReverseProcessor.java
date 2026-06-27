package io.nats.demo;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.function.Function;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class ReverseProcessor {

    @Bean
    public Function<Object, Object> transform() {
        return message -> {
            if (message instanceof byte[]) {
                String value = new String((byte[]) message, StandardCharsets.UTF_8);
                return reverseUppercase(value).getBytes(StandardCharsets.UTF_8);
            } else if (message instanceof String value) {
                return reverseUppercase(value).getBytes(StandardCharsets.UTF_8);
            }
            return message;
        };
    }

    private static String reverseUppercase(String value) {
        StringBuilder reverse = new StringBuilder();
        for (int i = value.length() - 1; i >= 0; i--) {
            reverse.append(value.charAt(i));
        }
        return reverse.toString().toUpperCase(Locale.ROOT);
    }
}
