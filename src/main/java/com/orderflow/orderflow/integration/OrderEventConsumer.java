package com.orderflow.orderflow.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderflow.orderflow.event.OrderEvent;
import org.slf4j.*;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);
    private final ObjectMapper objectMapper;
    public OrderEventConsumer(ObjectMapper objectMapper) { this.objectMapper = objectMapper; }
    @KafkaListener(topics = OrderEventPublisher.TOPIC)
    public void receive(String json) {
        try {
            OrderEvent event = objectMapper.readValue(json, OrderEvent.class);
            log.info("Received {} for order {}", event.eventType(), event.orderId());
        } catch (Exception exception) {
            log.warn("Received an unreadable order event");
        }
    }
}
