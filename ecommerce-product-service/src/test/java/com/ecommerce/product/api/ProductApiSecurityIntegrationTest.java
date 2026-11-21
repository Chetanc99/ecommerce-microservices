package com.ecommerce.product.api;

import com.ecommerce.common.security.JwtService;
import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductApiSecurityIntegrationTest {

    private static final String INTERNAL_KEY = "test-internal-key";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Test
    void catalogReadsArePublic() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(0));
    }

    @Test
    void missingProductReturnsProblemDetail() throws Exception {
        mockMvc.perform(get("/api/products/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Product not found"));
    }

    @Test
    void writingWithoutATokenIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson("Guarded", 10)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void writingAsANormalUserIsForbidden() throws Exception {
        String token = jwtService.generateToken(1L, "buyer@example.com", List.of("USER"));

        mockMvc.perform(post("/api/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson("Guarded", 10)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void adminCanCreateAndUpdateProducts() throws Exception {
        String token = jwtService.generateToken(2L, "admin@example.com", List.of("ADMIN"));
        long id = createProduct(token, productJson("Admin Keyboard", 10));

        mockMvc.perform(post("/api/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson("Admin Mouse", 4)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Admin Mouse"))
                .andExpect(jsonPath("$.price").value(19.99));

        mockMvc.perform(get("/api/products/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Admin Keyboard"));
    }

    @Test
    void invalidProductPayloadIsRejectedWithFieldErrors() throws Exception {
        String token = jwtService.generateToken(2L, "admin@example.com", List.of("ADMIN"));

        mockMvc.perform(post("/api/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","price":0,"quantity":-1,"category":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.price").exists())
                .andExpect(jsonPath("$.errors.quantity").exists())
                .andExpect(jsonPath("$.errors.category").exists());
    }

    @Test
    void internalStockEndpointsRequireTheApiKey() throws Exception {
        String token = jwtService.generateToken(2L, "admin@example.com", List.of("ADMIN"));
        long id = createProduct(token, productJson("Reserved Item", 9));

        mockMvc.perform(post("/internal/products/" + id + "/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quantity":2}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("A valid internal API key is required."));

        mockMvc.perform(post("/internal/products/" + id + "/reserve")
                        .header("X-Internal-Api-Key", INTERNAL_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quantity":2}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(7));
    }

    @Test
    void internalReservationBeyondStockReturnsConflict() throws Exception {
        String token = jwtService.generateToken(2L, "admin@example.com", List.of("ADMIN"));
        long id = createProduct(token, productJson("Scarce Item", 1));

        mockMvc.perform(post("/internal/products/" + id + "/reserve")
                        .header("X-Internal-Api-Key", INTERNAL_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quantity":5}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Insufficient stock"));
    }

    private long createProduct(String token, String body) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    private String productJson(String name, int quantity) {
        return """
                {"name":"%s","description":"Test product","price":19.99,"quantity":%d,"category":"Electronics"}
                """.formatted(name, quantity);
    }
}
