package com.ecommerce.product;

import com.ecommerce.common.security.JwtConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@EnableCaching
@Import(JwtConfiguration.class)
public class EcommerceProductServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EcommerceProductServiceApplication.class, args);
    }
}
