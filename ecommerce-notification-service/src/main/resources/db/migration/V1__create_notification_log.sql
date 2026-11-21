CREATE TABLE notification_log (
    event_id VARCHAR(36) NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    recipient VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    error_message VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (event_id),
    INDEX idx_notification_log_type (notification_type),
    INDEX idx_notification_log_status (status)
);
