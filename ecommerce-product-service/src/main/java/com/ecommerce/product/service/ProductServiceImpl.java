package com.ecommerce.product.service;

import com.ecommerce.product.dto.PageResponse;
import com.ecommerce.product.dto.ProductRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.dto.StockRequest;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.exception.InsufficientStockException;
import com.ecommerce.product.exception.ProductNotFoundException;
import com.ecommerce.product.repository.ProductRepository;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@Transactional
public class ProductServiceImpl implements ProductService {

    private static final String PRODUCT_CACHE = "products";

    private final ProductRepository productRepository;

    public ProductServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    @CacheEvict(cacheNames = PRODUCT_CACHE, allEntries = true)
    public ProductResponse createProduct(ProductRequest request) {

        Product product = new Product();
        applyRequest(product, request);

        return mapToResponse(productRepository.save(product));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getAllProducts(Pageable pageable) {
        return PageResponse.from(productRepository.findAll(pageable), this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = PRODUCT_CACHE, key = "#id")
    public ProductResponse getProductById(Long id) {
        return mapToResponse(findProduct(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getProductsByCategory(String category, Pageable pageable) {
        Page<Product> products = productRepository.findByCategoryIgnoreCase(category, pageable);
        return PageResponse.from(products, this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> searchProducts(String name, Pageable pageable) {
        Page<Product> products = productRepository.findByNameContainingIgnoreCase(name, pageable);
        return PageResponse.from(products, this::mapToResponse);
    }

    @Override
    @CacheEvict(cacheNames = PRODUCT_CACHE, allEntries = true)
    public ProductResponse updateProduct(Long id, ProductRequest request) {

        Product product = findProduct(id);
        applyRequest(product, request);

        return mapToResponse(productRepository.save(product));
    }

    @Override
    @CacheEvict(cacheNames = PRODUCT_CACHE, allEntries = true)
    public void deleteProduct(Long id) {
        productRepository.delete(findProduct(id));
    }

    @Override
    @CacheEvict(cacheNames = PRODUCT_CACHE, allEntries = true)
    public ProductResponse reserveStock(Long id, StockRequest request) {

        Product product = findProduct(id);

        if (product.getQuantity() < request.quantity()) {
            throw new InsufficientStockException(
                    "Product %d only has %d item(s) available".formatted(id, product.getQuantity())
            );
        }

        product.setQuantity(product.getQuantity() - request.quantity());

        return mapToResponse(productRepository.save(product));
    }

    @Override
    @CacheEvict(cacheNames = PRODUCT_CACHE, allEntries = true)
    public void releaseStock(Long id, StockRequest request) {

        Product product = findProduct(id);
        product.setQuantity(product.getQuantity() + request.quantity());

        productRepository.save(product);
    }

    private Product findProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));
    }

    private void applyRequest(Product product, ProductRequest request) {
        product.setName(request.getName().trim());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice().setScale(2, RoundingMode.HALF_UP));
        product.setQuantity(request.getQuantity());
        product.setCategory(request.getCategory().trim());
        product.setImageUrl(request.getImageUrl());
    }

    private ProductResponse mapToResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getQuantity(),
                product.getCategory(),
                product.getImageUrl(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
