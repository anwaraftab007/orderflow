package com.orderflow.orderflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderflow.orderflow.dto.ProductResponse;
import com.orderflow.orderflow.event.InventoryChangedEvent;
import com.orderflow.orderflow.integration.RedisOperationException;
import com.orderflow.orderflow.integration.UpstashRedisClient;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import java.util.Optional;

@Service
public class ProductCacheService {
    private static final Logger log = LoggerFactory.getLogger(ProductCacheService.class);
    private final UpstashRedisClient redis;
    private final ObjectMapper objectMapper;
    private final long ttlSeconds;
    public ProductCacheService(UpstashRedisClient redis, ObjectMapper objectMapper,
                               @Value("${redis.product-ttl-seconds}") long ttlSeconds) {
        this.redis = redis; this.objectMapper = objectMapper; this.ttlSeconds = ttlSeconds;
    }
    public Optional<ProductResponse> get(Long id) {
        try {
            String value = redis.get(key(id));
            return value == null ? Optional.empty() : Optional.of(objectMapper.readValue(value, ProductResponse.class));
        } catch (Exception exception) {
            log.warn("Product cache read unavailable; falling back to PostgreSQL");
            return Optional.empty();
        }
    }
    public void put(ProductResponse product) {
        try { redis.set(key(product.id()), objectMapper.writeValueAsString(product), ttlSeconds); }
        catch (Exception exception) { log.warn("Product cache write unavailable; continuing without cache"); }
    }
    @TransactionalEventListener
    public void invalidateAfterCommit(InventoryChangedEvent event) {
        for (Long id : event.productIds()) try { redis.delete(key(id)); }
        catch (RedisOperationException exception) { log.warn("Product cache invalidation unavailable for product {}", id); }
    }
    private String key(Long id) { return "product:" + id; }
}
