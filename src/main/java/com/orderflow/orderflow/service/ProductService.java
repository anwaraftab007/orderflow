package com.orderflow.orderflow.service;

import com.orderflow.orderflow.dto.ProductCreateRequest;
import com.orderflow.orderflow.dto.ProductResponse;
import com.orderflow.orderflow.entity.Inventory;
import com.orderflow.orderflow.entity.Product;
import com.orderflow.orderflow.exception.BusinessException;
import com.orderflow.orderflow.repository.InventoryRepository;
import com.orderflow.orderflow.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final ProductCacheService productCacheService;

    public ProductService(ProductRepository productRepository, InventoryRepository inventoryRepository,
                          ProductCacheService productCacheService) {
        this.productRepository = productRepository; this.inventoryRepository = inventoryRepository;
        this.productCacheService = productCacheService;
    }

    @Transactional
    public ProductResponse create(ProductCreateRequest request) {
        Product product = productRepository.save(new Product(request.name().trim(), request.description(), request.price()));
        Inventory inventory = inventoryRepository.save(new Inventory(product, request.initialQuantity()));
        return toResponse(product, inventory);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> findActive() {
        return productRepository.findByActiveTrueOrderByNameAsc().stream().map(this::withInventory).toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(Long id) {
        var cached = productCacheService.get(id);
        if (cached.isPresent()) return cached.get();
        Product product = productRepository.findById(id)
                .filter(Product::isActive)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Product not found"));
        ProductResponse response = withInventory(product);
        productCacheService.put(response);
        return response;
    }

    private ProductResponse withInventory(Product product) {
        Inventory inventory = inventoryRepository.findByProductId(product.getId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Inventory not found"));
        return toResponse(product, inventory);
    }

    private ProductResponse toResponse(Product product, Inventory inventory) {
        return new ProductResponse(product.getId(), product.getName(), product.getDescription(),
                product.getPrice(), inventory.getQuantity(), product.isActive());
    }
}
