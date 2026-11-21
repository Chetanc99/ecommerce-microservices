package com.ecommerce.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class EcommerceNotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EcommerceNotificationServiceApplication.class, args);
    }
}
