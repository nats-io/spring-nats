package io.nats.cloud.stream.binder;

import io.nats.client.Message;
import io.nats.client.impl.Headers;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.GenericMessage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NatsMessageConverter {
    public static Map<String, Object> fromNatsHeaders(Message m) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(NatsMessageProducer.SUBJECT, m.getSubject());
        headers.put(MessageHeaders.REPLY_CHANNEL, m.getReplyTo());
        if (m.getHeaders() != null) {
            for (Map.Entry<String, List<String>> entry : m.getHeaders().entrySet()) {
                if (entry.getValue().size() == 1) {
                    headers.put(entry.getKey(), entry.getValue().get(0));
                } else if (entry.getValue().size() > 1) {
                    headers.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return headers;
    }
    public static GenericMessage<byte[]> natsMessageToGenericMessage(Message msg) {
        Map<String, Object> headers = fromNatsHeaders(msg);
        GenericMessage<byte[]> m = new GenericMessage<byte[]>(msg.getData(), headers);
        return m;
    }

    public static Headers toNatsHeaders(org.springframework.messaging.Message<?> message) {
        Headers headers = new Headers();
        for (String key : message.getHeaders().keySet()) {
            Object o = message.getHeaders().get(key);
            if (o instanceof List l) {
                if (l.size() == 1) {
                    headers.put(key, (String) l.get(0));
                } else {
                    headers.put(key, l);
                }
            } else if (o instanceof String) {
                headers.put(key, (String) o);
            }
        }
        return headers;
    }
}
