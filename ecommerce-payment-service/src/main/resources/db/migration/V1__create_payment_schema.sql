CREATE TABLE payments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    user_email VARCHAR(255) NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    razorpay_order_id VARCHAR(100) NOT NULL,
    razorpay_payment_id VARCHAR(100) NULL,
    failure_reason VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_payments_razorpay_order_id UNIQUE (razorpay_order_id),
    CONSTRAINT uk_payments_razorpay_payment_id UNIQUE (razorpay_payment_id),
    INDEX idx_payments_order_id (order_id),
    INDEX idx_payments_user_id (user_id)
);

CREATE TABLE payment_outbox (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_id VARCHAR(36) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    topic VARCHAR(100) NOT NULL,
    payload LONGTEXT NOT NULL,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL,
    published_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_payment_outbox_event_id UNIQUE (event_id),
    INDEX idx_payment_outbox_published (published, created_at)
);

CREATE TABLE processed_webhooks (
    event_id VARCHAR(100) NOT NULL,
    processed_at DATETIME(6) NOT NULL,
    PRIMARY KEY (event_id)
);
