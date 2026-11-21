package com.ecommerce.gateway;

import com.ecommerce.common.security.JwtConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(JwtConfiguration.class)
public class EcommerceApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(EcommerceApiGatewayApplication.class, args);
    }
}
