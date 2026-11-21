package com.ecommerce.order;

import com.ecommerce.common.security.JwtConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableKafka
@EnableScheduling
@Import(JwtConfiguration.class)
public class EcommerceOrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EcommerceOrderServiceApplication.class, args);
    }
}
