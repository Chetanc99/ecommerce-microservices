package com.ecommerce.notification.service;

import com.ecommerce.notification.email.EmailSender;
import com.ecommerce.notification.email.NotificationMessage;
import com.ecommerce.notification.entity.NotificationLog;
import com.ecommerce.notification.entity.NotificationStatus;
import com.ecommerce.notification.repository.NotificationLogRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private static final String EVENT_ID = "evt-notification-1";

    @Mock
    private NotificationLogRepository logRepository;

    @Mock
    private EmailSender emailSender;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void duplicateEventsAreNotDeliveredAgain() {
        when(logRepository.existsById(EVENT_ID)).thenReturn(true);

        notificationService.deliver(message("buyer@example.com"));

        verifyNoInteractions(emailSender);
        verify(logRepository, never()).save(any());
    }

    @Test
    void notificationsWithoutARecipientAreSkippedAndRecorded() {
        when(logRepository.existsById(EVENT_ID)).thenReturn(false);

        notificationService.deliver(message("  "));

        verifyNoInteractions(emailSender);

        NotificationLog stored = capture();
        assertThat(stored.getStatus()).isEqualTo(NotificationStatus.SKIPPED);
        assertThat(stored.getRecipient()).isBlank();
        assertThat(stored.getErrorMessage()).isEqualTo("No recipient e-mail");
    }

    @Test
    void aNullRecipientIsRecordedAsAnEmptyString() {
        when(logRepository.existsById(EVENT_ID)).thenReturn(false);

        notificationService.deliver(message(null));

        verifyNoInteractions(emailSender);
        assertThat(capture().getRecipient()).isEmpty();
    }

    @Test
    void disabledMailDeliveryIsRecordedAsSkipped() {
        when(logRepository.existsById(EVENT_ID)).thenReturn(false);
        when(emailSender.send("buyer@example.com", "Subject", "Body")).thenReturn(false);

        notificationService.deliver(message("buyer@example.com"));

        NotificationLog stored = capture();
        assertThat(stored.getStatus()).isEqualTo(NotificationStatus.SKIPPED);
        assertThat(stored.getErrorMessage()).isEqualTo("Mail delivery disabled");
    }

    @Test
    void aSuccessfulSendIsRecordedWithoutAnError() {
        when(logRepository.existsById(EVENT_ID)).thenReturn(false);
        when(emailSender.send("buyer@example.com", "Subject", "Body")).thenReturn(true);

        notificationService.deliver(message("buyer@example.com"));

        NotificationLog stored = capture();
        assertThat(stored.getEventId()).isEqualTo(EVENT_ID);
        assertThat(stored.getNotificationType()).isEqualTo("ORDER_CONFIRMED");
        assertThat(stored.getRecipient()).isEqualTo("buyer@example.com");
        assertThat(stored.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(stored.getErrorMessage()).isNull();
    }

    @Test
    void deliveryFailuresAreRecordedWithTheTruncatedReason() {
        when(logRepository.existsById(EVENT_ID)).thenReturn(false);
        when(emailSender.send(anyString(), anyString(), anyString()))
                .thenThrow(new IllegalStateException("smtp".repeat(200)));

        notificationService.deliver(message("buyer@example.com"));

        NotificationLog stored = capture();
        assertThat(stored.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(stored.getErrorMessage()).hasSize(500);
    }

    @Test
    void aFailureWithoutAMessageFallsBackToADefaultReason() {
        when(logRepository.existsById(EVENT_ID)).thenReturn(false);
        when(emailSender.send(anyString(), anyString(), anyString()))
                .thenThrow(new IllegalStateException());

        notificationService.deliver(message("buyer@example.com"));

        NotificationLog stored = capture();
        assertThat(stored.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(stored.getErrorMessage()).isEqualTo("Unknown mail error");
    }

    private NotificationLog capture() {
        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(logRepository).save(captor.capture());
        return captor.getValue();
    }

    private NotificationMessage message(String recipient) {
        return new NotificationMessage(EVENT_ID, "ORDER_CONFIRMED", recipient, "Subject", "Body");
    }
}
