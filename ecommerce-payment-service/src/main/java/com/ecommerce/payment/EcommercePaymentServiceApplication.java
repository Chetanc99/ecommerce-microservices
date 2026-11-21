package com.ecommerce.payment;

import com.ecommerce.common.security.JwtConfiguration;
import com.ecommerce.payment.config.RazorpayProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(RazorpayProperties.class)
@Import(JwtConfiguration.class)
public class EcommercePaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EcommercePaymentServiceApplication.class, args);
    }
}
