package com.orderflow.orderflow.service;

import com.orderflow.orderflow.dto.CreateOrderRequest;
import com.orderflow.orderflow.dto.OrderResponse;
import com.orderflow.orderflow.entity.*;
import com.orderflow.orderflow.exception.BusinessException;
import com.orderflow.orderflow.event.InventoryChangedEvent;
import com.orderflow.orderflow.event.OrderEvent;
import com.orderflow.orderflow.event.OrderEventType;
import com.orderflow.orderflow.repository.InventoryRepository;
import com.orderflow.orderflow.repository.OrderRepository;
import com.orderflow.orderflow.repository.ProductRepository;
import com.orderflow.orderflow.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository,
                        InventoryRepository inventoryRepository, UserRepository userRepository,
                        ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository; this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository; this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public OrderResponse create(CreateOrderRequest request, String email) {
        rejectDuplicateProducts(request);
        User user = findUser(email);
        CustomerOrder order = new CustomerOrder(user);

        for (CreateOrderRequest.Item requested : request.items()) {
            Product product = productRepository.findById(requested.productId())
                    .filter(Product::isActive)
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Product not found: " + requested.productId()));
            // PostgreSQL checks and decrements in one atomic statement; this prevents concurrent overselling.
            int updatedRows = inventoryRepository.decreaseStock(product.getId(), requested.quantity());
            if (updatedRows == 0) {
                throw new BusinessException(HttpStatus.CONFLICT, "Insufficient stock for product: " + product.getName());
            }
            order.addItem(new OrderItem(order, product, requested.quantity(), product.getPrice()));
        }
        CustomerOrder saved = orderRepository.save(order);
        Set<Long> productIds = request.items().stream().map(CreateOrderRequest.Item::productId).collect(java.util.stream.Collectors.toSet());
        eventPublisher.publishEvent(new InventoryChangedEvent(productIds));
        eventPublisher.publishEvent(OrderEvent.create(saved.getId(), user.getId(), OrderEventType.ORDER_CREATED, saved.getTotalAmount()));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> findMine(String email) {
        return orderRepository.findByUserEmailOrderByCreatedAtDesc(email).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse findById(Long id, String email) {
        User requester = findUser(email);
        CustomerOrder order = findOrder(id);
        verifyOwnerOrAdmin(order, requester);
        return toResponse(order);
    }

    @Transactional
    public OrderResponse cancel(Long id, String email) {
        User requester = findUser(email);
        CustomerOrder order = findOrder(id);
        verifyOwnerOrAdmin(order, requester);
        int claimed = orderRepository.markCancelledIfEligible(
                id, OrderStatus.CANCELLED, List.of(OrderStatus.CREATED, OrderStatus.CONFIRMED));
        if (claimed == 0) {
            throw new BusinessException(HttpStatus.CONFLICT, "Order cannot be cancelled from status " + order.getStatus());
        }
        for (OrderItem item : order.getItems()) {
            int updatedRows = inventoryRepository.increaseStock(item.getProduct().getId(), item.getQuantity());
            if (updatedRows == 0) throw new BusinessException(HttpStatus.NOT_FOUND, "Inventory not found");
        }
        order.changeStatus(OrderStatus.CANCELLED);
        eventPublisher.publishEvent(new InventoryChangedEvent(order.getItems().stream()
                .map(item -> item.getProduct().getId()).collect(java.util.stream.Collectors.toSet())));
        eventPublisher.publishEvent(OrderEvent.create(order.getId(), order.getUser().getId(),
                OrderEventType.ORDER_CANCELLED, order.getTotalAmount()));
        return toResponse(order);
    }

    @Transactional
    public OrderResponse updateStatus(Long id, OrderStatus requestedStatus) {
        CustomerOrder order = findOrder(id);
        OrderStatus expected = switch (order.getStatus()) {
            case CREATED -> OrderStatus.CONFIRMED;
            case CONFIRMED -> OrderStatus.SHIPPED;
            case SHIPPED -> OrderStatus.DELIVERED;
            case DELIVERED, CANCELLED -> null;
        };
        if (requestedStatus != expected) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "Invalid order status transition: " + order.getStatus() + " -> " + requestedStatus);
        }
        order.changeStatus(requestedStatus);
        CustomerOrder saved = orderRepository.save(order);
        OrderEventType eventType = switch (requestedStatus) {
            case CONFIRMED -> OrderEventType.ORDER_CONFIRMED;
            case SHIPPED -> OrderEventType.ORDER_SHIPPED;
            case DELIVERED -> OrderEventType.ORDER_DELIVERED;
            default -> throw new IllegalStateException("Unsupported status event");
        };
        eventPublisher.publishEvent(OrderEvent.create(saved.getId(), saved.getUser().getId(), eventType, saved.getTotalAmount()));
        return toResponse(saved);
    }

    private void rejectDuplicateProducts(CreateOrderRequest request) {
        Set<Long> ids = new HashSet<>();
        if (request.items().stream().anyMatch(item -> !ids.add(item.productId()))) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Duplicate product IDs are not allowed");
        }
    }
    private User findUser(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
    private CustomerOrder findOrder(Long id) {
        return orderRepository.findById(id).orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Order not found"));
    }
    private void verifyOwnerOrAdmin(CustomerOrder order, User requester) {
        if (requester.getRole() != Role.ADMIN && !order.getUser().getId().equals(requester.getId())) throw new AccessDeniedException("Access denied");
    }
    private OrderResponse toResponse(CustomerOrder order) {
        List<OrderResponse.Item> items = order.getItems().stream().map(item -> new OrderResponse.Item(
                item.getProduct().getId(), item.getProduct().getName(), item.getQuantity(),
                item.getUnitPrice(), item.getLineTotal())).toList();
        return new OrderResponse(order.getId(), order.getStatus(), order.getTotalAmount(), order.getCreatedAt(), items);
    }
}
