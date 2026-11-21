package com.ecommerce.common.events;

public final class KafkaTopics {

    public static final String ORDER_CREATED = "order.created";
    public static final String PAYMENT_INITIATED = "payment.initiated";
    public static final String PAYMENT_SUCCESSFUL = "payment.successful";
    public static final String PAYMENT_FAILED = "payment.failed";
    public static final String ORDER_CONFIRMED = "order.confirmed";
    public static final String ORDER_CANCELLED = "order.cancelled";

    public static final String DEAD_LETTER_SUFFIX = ".DLT";

    private KafkaTopics() {
    }

    public static String deadLetter(String topic) {
        return topic + DEAD_LETTER_SUFFIX;
    }
}
