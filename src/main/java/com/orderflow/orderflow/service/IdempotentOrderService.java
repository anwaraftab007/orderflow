package com.orderflow.orderflow.service;

import com.orderflow.orderflow.dto.CreateOrderRequest;
import com.orderflow.orderflow.dto.OrderResponse;
import com.orderflow.orderflow.exception.BusinessException;
import com.orderflow.orderflow.integration.RedisOperationException;
import com.orderflow.orderflow.integration.UpstashRedisClient;
import com.orderflow.orderflow.repository.UserRepository;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class IdempotentOrderService {
    private static final Logger log = LoggerFactory.getLogger(IdempotentOrderService.class);
    private final UpstashRedisClient redis;
    private final OrderService orderService;
    private final UserRepository userRepository;
    private final long ttlSeconds;
    public IdempotentOrderService(UpstashRedisClient redis, OrderService orderService, UserRepository userRepository,
                                  @Value("${redis.idempotency-ttl-seconds}") long ttlSeconds) {
        this.redis = redis; this.orderService = orderService; this.userRepository = userRepository; this.ttlSeconds = ttlSeconds;
    }

    public OrderResponse create(CreateOrderRequest request, String email, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 100) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Idempotency-Key must be between 1 and 100 characters");
        }
        Long userId = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "User not found")).getId();
        String key = "idempotency:order:" + userId + ":" + idempotencyKey.trim();
        String processing = "PROCESSING:" + UUID.randomUUID();
        try {
            boolean reserved = redis.setIfAbsent(key, processing, ttlSeconds);
            String existing = reserved ? processing : redis.get(key);
            if (!reserved && !processing.equals(existing)) {
                if (existing != null && existing.startsWith("COMPLETED:")) {
                    return orderService.findById(Long.parseLong(existing.substring("COMPLETED:".length())), email);
                }
                throw new BusinessException(HttpStatus.CONFLICT, "A request with this Idempotency-Key is still processing");
            }

            OrderResponse response;
            try {
                response = orderService.create(request, email);
            } catch (RuntimeException exception) {
                try { redis.compareAndDelete(key, processing); }
                catch (RedisOperationException cleanupFailure) { log.warn("Could not clear failed idempotency reservation"); }
                throw exception;
            }

            // The database transaction has committed. A Redis completion failure must not turn
            // this successful order into a client-visible failure that encourages a blind retry.
            try {
                if (!redis.compareAndSet(key, processing, "COMPLETED:" + response.orderId(), ttlSeconds)) {
                    log.error("Could not complete idempotency record for order {}", response.orderId());
                }
            } catch (RedisOperationException exception) {
                log.error("Could not complete idempotency record for order {}", response.orderId());
            }
            return response;
        } catch (RedisOperationException exception) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "Idempotency service is temporarily unavailable");
        }
    }
}
