package com.ecommerce.server;

import com.ecommerce.common.security.JwtConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(JwtConfiguration.class)
public class EcommercePlatformServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EcommercePlatformServerApplication.class, args);
    }
}
