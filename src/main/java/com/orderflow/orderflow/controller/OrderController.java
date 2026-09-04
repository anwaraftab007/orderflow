package com.orderflow.orderflow.controller;

import com.orderflow.orderflow.dto.*;
import com.orderflow.orderflow.service.IdempotentOrderService;
import com.orderflow.orderflow.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;
    private final IdempotentOrderService idempotentOrderService;
    public OrderController(OrderService orderService, IdempotentOrderService idempotentOrderService) {
        this.orderService = orderService; this.idempotentOrderService = idempotentOrderService;
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@Valid @RequestBody CreateOrderRequest request,
                                @RequestHeader("Idempotency-Key") String idempotencyKey, Authentication auth) {
        return idempotentOrderService.create(request, auth.getName(), idempotencyKey);
    }
    @GetMapping("/my") public List<OrderResponse> mine(Authentication auth) { return orderService.findMine(auth.getName()); }
    @GetMapping("/{id}") public OrderResponse find(@PathVariable Long id, Authentication auth) { return orderService.findById(id, auth.getName()); }
    @PatchMapping("/{id}/cancel") public OrderResponse cancel(@PathVariable Long id, Authentication auth) { return orderService.cancel(id, auth.getName()); }
}
