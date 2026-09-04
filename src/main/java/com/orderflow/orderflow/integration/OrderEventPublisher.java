package com.orderflow.orderflow.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderflow.orderflow.event.OrderEvent;
import org.slf4j.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import java.util.concurrent.TimeUnit;

@Component
public class OrderEventPublisher {
    public static final String TOPIC = "order-events";
    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    public OrderEventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate; this.objectMapper = objectMapper;
    }

    @TransactionalEventListener
    public void publishAfterCommit(OrderEvent event) {
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                kafkaTemplate.send(TOPIC, event.orderId().toString(), objectMapper.writeValueAsString(event))
                        .get(5, TimeUnit.SECONDS);
                log.info("Published {} for order {}", event.eventType(), event.orderId());
                return;
            } catch (Exception exception) {
                if (attempt < 3) {
                    try { Thread.sleep(200L * attempt); } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt(); break;
                    }
                }
            }
        }
        log.error("Could not publish {} for order {} after 3 attempts", event.eventType(), event.orderId());
    }
}
