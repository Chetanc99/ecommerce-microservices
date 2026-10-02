package com.ecommerce.product.service;

import com.ecommerce.product.dto.ProductRequest;
import com.ecommerce.product.dto.ProductResponse;

import java.util.List;

public interface ProductService {

    ProductResponse createProduct(ProductRequest request);

    List<ProductResponse> getAllProducts();

    ProductResponse getProductById(Long id);

    List<ProductResponse> getProductsByCategory(String category);

    List<ProductResponse> searchProducts(String name);

    ProductResponse updateProduct(
            Long id,
            ProductRequest request
    );

    void deleteProduct(Long id);
}