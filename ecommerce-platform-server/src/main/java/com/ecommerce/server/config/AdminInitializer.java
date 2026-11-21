package com.ecommerce.server.config;

import com.ecommerce.server.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final UserService userService;
    private final String adminEmail;
    private final String adminPassword;

    public AdminInitializer(
            UserService userService,
            @Value("${app.admin.email:}") String adminEmail,
            @Value("${app.admin.password:}") String adminPassword) {

        this.userService = userService;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        userService.ensureAdmin(adminEmail, adminPassword);

        if (adminEmail != null && !adminEmail.isBlank()) {
            log.info("Admin account ensured for {}", adminEmail);
        }
    }
}
