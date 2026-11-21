package com.ecommerce.notification.email;

public record NotificationMessage(
        String eventId,
        String type,
        String recipient,
        String subject,
        String body
) {
}
