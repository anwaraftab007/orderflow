package com.orderflow.orderflow.controller;

import com.orderflow.orderflow.dto.ProductCreateRequest;
import com.orderflow.orderflow.dto.ProductResponse;
import com.orderflow.orderflow.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/products")
public class ProductController {
    private final ProductService productService;
    public ProductController(ProductService productService) { this.productService = productService; }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @RequestBody ProductCreateRequest request) { return productService.create(request); }
    @GetMapping public List<ProductResponse> findAll() { return productService.findActive(); }
    @GetMapping("/{id}") public ProductResponse findById(@PathVariable Long id) { return productService.findById(id); }
}
