package com.ecommerce.product.service;

import com.ecommerce.product.dto.PageResponse;
import com.ecommerce.product.dto.ProductRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.dto.StockRequest;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.exception.InsufficientStockException;
import com.ecommerce.product.exception.ProductNotFoundException;
import com.ecommerce.product.repository.ProductRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    void createProductTrimsTextAndRoundsPriceToTwoDecimals() {
        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse response = productService.createProduct(
                request("  Mechanical Keyboard  ", "  Electronics  ", new BigDecimal("79.999"), 10));

        assertThat(response.name()).isEqualTo("Mechanical Keyboard");
        assertThat(response.category()).isEqualTo("Electronics");
        assertThat(response.price()).isEqualByComparingTo("80.00");
        assertThat(response.price().scale()).isEqualTo(2);
        assertThat(response.quantity()).isEqualTo(10);
    }

    @Test
    void getProductByIdReturnsCachedShapeAndFailsForUnknownId() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product(1L, "Mouse", 5)));

        ProductResponse response = productService.getProductById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Mouse");

        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(99L))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void getAllProductsMapsPaginationMetadata() {
        Pageable pageable = PageRequest.of(1, 2);
        when(productRepository.findAll(pageable)).thenReturn(
                new PageImpl<>(List.of(product(1L, "Mouse", 5), product(2L, "Pad", 3)),
                        pageable, 7));

        PageResponse<ProductResponse> response = productService.getAllProducts(pageable);

        assertThat(response.content()).hasSize(2);
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.totalElements()).isEqualTo(7);
        assertThat(response.first()).isFalse();
    }

    @Test
    void getProductsByCategoryUsesIgnoreCaseLookup() {
        Pageable pageable = PageRequest.of(0, 12);
        when(productRepository.findByCategoryIgnoreCase("electronics", pageable))
                .thenReturn(new PageImpl<>(List.of(product(1L, "Mouse", 5)), pageable, 1));

        PageResponse<ProductResponse> response =
                productService.getProductsByCategory("electronics", pageable);

        assertThat(response.content()).singleElement()
                .extracting(ProductResponse::name)
                .isEqualTo("Mouse");
    }

    @Test
    void searchProductsUsesPartialNameLookup() {
        Pageable pageable = PageRequest.of(0, 12);
        when(productRepository.findByNameContainingIgnoreCase("key", pageable))
                .thenReturn(new PageImpl<>(List.of(product(1L, "Keyboard", 5)), pageable, 1));

        assertThat(productService.searchProducts("key", pageable).content())
                .singleElement()
                .extracting(ProductResponse::name)
                .isEqualTo("Keyboard");
    }

    @Test
    void updateProductAppliesAllEditableFields() {
        Product existing = product(1L, "Old name", 1);
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.save(existing)).thenReturn(existing);

        ProductResponse response = productService.updateProduct(1L,
                request("New name", "Books", new BigDecimal("12.50"), 42));

        assertThat(response.name()).isEqualTo("New name");
        assertThat(response.category()).isEqualTo("Books");
        assertThat(response.price()).isEqualByComparingTo("12.50");
        assertThat(response.quantity()).isEqualTo(42);
    }

    @Test
    void updateProductFailsForUnknownIdWithoutSaving() {
        when(productRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateProduct(5L,
                request("Any", "Books", BigDecimal.ONE, 1)))
                .isInstanceOf(ProductNotFoundException.class);

        verify(productRepository, never()).save(any());
    }

    @Test
    void deleteProductRemovesTheLoadedEntityOnly() {
        Product existing = product(3L, "To delete", 1);
        when(productRepository.findById(3L)).thenReturn(Optional.of(existing));

        productService.deleteProduct(3L);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).delete(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(3L);
    }

    @Test
    void deleteProductFailsForUnknownId() {
        when(productRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.deleteProduct(9L))
                .isInstanceOf(ProductNotFoundException.class);

        verify(productRepository, never()).delete(any());
    }

    @Test
    void reserveStockDecrementsAvailableQuantity() {
        Product existing = product(1L, "Keyboard", 10);
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.save(existing)).thenReturn(existing);

        ProductResponse response = productService.reserveStock(1L, new StockRequest(3));

        assertThat(response.quantity()).isEqualTo(7);
        assertThat(existing.getQuantity()).isEqualTo(7);
    }

    @Test
    void reserveStockRejectsOrdersLargerThanAvailableStock() {
        Product existing = product(1L, "Keyboard", 2);
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> productService.reserveStock(1L, new StockRequest(3)))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("only has 2 item(s) available");

        assertThat(existing.getQuantity()).isEqualTo(2);
        verify(productRepository, never()).save(any());
    }

    @Test
    void reserveStockFailsForUnknownProduct() {
        when(productRepository.findById(77L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.reserveStock(77L, new StockRequest(1)))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void releaseStockAddsQuantityBack() {
        Product existing = product(1L, "Keyboard", 4);
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.save(existing)).thenReturn(existing);

        productService.releaseStock(1L, new StockRequest(2));

        assertThat(existing.getQuantity()).isEqualTo(6);
    }

    private ProductRequest request(String name, String category, BigDecimal price, int quantity) {
        ProductRequest request = new ProductRequest();
        request.setName(name);
        request.setCategory(category);
        request.setPrice(price);
        request.setQuantity(quantity);
        request.setDescription("Description");
        request.setImageUrl("https://example.com/image.png");
        return request;
    }

    private Product product(Long id, String name, int quantity) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setCategory("Electronics");
        product.setPrice(new BigDecimal("19.99"));
        product.setQuantity(quantity);
        return product;
    }
}
