package com.ecommerce.product.cache;

import com.ecommerce.product.dto.ProductRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.dto.StockRequest;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.repository.ProductRepository;
import com.ecommerce.product.service.ProductService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ProductCacheIntegrationTest {

    private static final String CACHE_NAME = "products";

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CacheManager cacheManager;

    private Long productId;

    @BeforeEach
    void setUp() {
        cacheManager.getCache(CACHE_NAME).clear();

        Product product = new Product();
        product.setName("Cached Keyboard");
        product.setCategory("Electronics");
        product.setPrice(new BigDecimal("49.99"));
        product.setQuantity(12);

        productId = productRepository.save(product).getId();
    }

    @Test
    void readingAProductPopulatesTheCache() {
        productService.getProductById(productId);

        Cache cache = cacheManager.getCache(CACHE_NAME);
        ProductResponse cached = cache.get(productId, ProductResponse.class);

        assertThat(cached).isNotNull();
        assertThat(cached.name()).isEqualTo("Cached Keyboard");
    }

    @Test
    void updatingAProductEvictsCachedEntries() {
        productService.getProductById(productId);
        assertThat(cacheManager.getCache(CACHE_NAME).get(productId)).isNotNull();

        productService.updateProduct(productId, request("Updated Keyboard", 8));

        assertThat(cacheManager.getCache(CACHE_NAME).get(productId)).isNull();
    }

    @Test
    void reservingStockEvictsCachedEntriesSoAvailabilityIsFresh() {
        productService.getProductById(productId);

        productService.reserveStock(productId, new StockRequest(2));

        assertThat(cacheManager.getCache(CACHE_NAME).get(productId)).isNull();

        ProductResponse reloaded = productService.getProductById(productId);
        assertThat(reloaded.quantity()).isEqualTo(10);
    }

    @Test
    void releasingStockEvictsCachedEntries() {
        productService.reserveStock(productId, new StockRequest(2));
        productService.getProductById(productId);

        productService.releaseStock(productId, new StockRequest(1));

        assertThat(cacheManager.getCache(CACHE_NAME).get(productId)).isNull();
        assertThat(productService.getProductById(productId).quantity()).isEqualTo(11);
    }

    private ProductRequest request(String name, int quantity) {
        ProductRequest request = new ProductRequest();
        request.setName(name);
        request.setCategory("Electronics");
        request.setPrice(new BigDecimal("59.99"));
        request.setQuantity(quantity);
        return request;
    }
}
