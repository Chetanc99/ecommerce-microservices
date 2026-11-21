package com.ecommerce.product.controller;

import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.dto.StockRequest;
import com.ecommerce.product.service.ProductService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/products")
public class InternalProductController {

    private final ProductService productService;

    public InternalProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping("/{id}/reserve")
    public ResponseEntity<ProductResponse> reserveStock(
            @PathVariable Long id,
            @Valid @RequestBody StockRequest request) {

        return ResponseEntity.ok(productService.reserveStock(id, request));
    }

    @PostMapping("/{id}/release")
    public ResponseEntity<Void> releaseStock(
            @PathVariable Long id,
            @Valid @RequestBody StockRequest request) {

        productService.releaseStock(id, request);
        return ResponseEntity.noContent().build();
    }
}
