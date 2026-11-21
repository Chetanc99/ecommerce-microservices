package com.ecommerce.payment.service;

import com.ecommerce.common.security.JwtPrincipal;
import com.ecommerce.payment.client.OrderClient;
import com.ecommerce.payment.config.RazorpayProperties;
import com.ecommerce.payment.dto.CreatePaymentRequest;
import com.ecommerce.payment.dto.OrderSummary;
import com.ecommerce.payment.dto.PaymentOrderResponse;
import com.ecommerce.payment.dto.PaymentStatusResponse;
import com.ecommerce.payment.dto.VerifyPaymentRequest;
import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.entity.PaymentStatus;
import com.ecommerce.payment.exception.PaymentAccessDeniedException;
import com.ecommerce.payment.exception.PaymentConfigurationException;
import com.ecommerce.payment.exception.PaymentNotFoundException;
import com.ecommerce.payment.exception.PaymentStateException;
import com.ecommerce.payment.exception.PaymentVerificationException;
import com.ecommerce.payment.razorpay.RazorpayGateway;
import com.ecommerce.payment.razorpay.RazorpayOrderResult;
import com.ecommerce.payment.razorpay.RazorpaySignatureVerifier;
import com.ecommerce.payment.razorpay.RazorpayWebhookEvent;
import com.ecommerce.payment.razorpay.RazorpayWebhookParser;
import com.ecommerce.payment.repository.PaymentRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    private static final JwtPrincipal BUYER =
            new JwtPrincipal(7L, "buyer@example.com", List.of("USER"));
    private static final String RAZORPAY_ORDER_ID = "order_ABC23";

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentPersistenceService persistenceService;

    @Mock
    private PaymentSettlementService settlementService;

    @Mock
    private PaymentWebhookHandler webhookHandler;

    @Mock
    private OrderClient orderClient;

    @Mock
    private RazorpayGateway razorpayGateway;

    @Mock
    private RazorpaySignatureVerifier signatureVerifier;

    @Mock
    private RazorpayWebhookParser webhookParser;

    private PaymentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = createService(configuredProperties());
    }

    @Test
    void createPaymentRejectsOrdersThatAreNotAwaitingPayment() {
        when(orderClient.getOrderSummary(1L, 7L))
                .thenReturn(order("CONFIRMED"));

        assertThatThrownBy(() -> service.createPayment(BUYER, new CreatePaymentRequest(1L)))
                .isInstanceOf(PaymentStateException.class)
                .hasMessageContaining("not awaiting payment");

        verifyNoInteractions(paymentRepository, razorpayGateway);
    }

    @Test
    void createPaymentRefusesToChargeAnAlreadyPaidOrder() {
        when(orderClient.getOrderSummary(1L, 7L)).thenReturn(order("PENDING_PAYMENT"));
        when(paymentRepository.findByOrderIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(payment(PaymentStatus.SUCCESS)));

        assertThatThrownBy(() -> service.createPayment(BUYER, new CreatePaymentRequest(1L)))
                .isInstanceOf(PaymentStateException.class)
                .hasMessageContaining("already been paid");

        verifyNoInteractions(razorpayGateway);
    }

    @Test
    void createPaymentReusesAnUnfinishedAttempt() {
        Payment reusable = payment(PaymentStatus.CREATED);
        when(orderClient.getOrderSummary(1L, 7L)).thenReturn(order("PENDING_PAYMENT"));
        when(paymentRepository.findByOrderIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        when(paymentRepository.findFirstByOrderIdAndStatusInOrderByCreatedAtDesc(
                1L, List.of(PaymentStatus.CREATED, PaymentStatus.PENDING)))
                .thenReturn(Optional.of(reusable));

        PaymentOrderResponse response =
                service.createPayment(BUYER, new CreatePaymentRequest(1L));

        assertThat(response.razorpayOrderId()).isEqualTo(RAZORPAY_ORDER_ID);
        assertThat(response.amount()).isEqualByComparingTo("59.98");
        verifyNoInteractions(razorpayGateway, persistenceService);
    }

    @Test
    void createPaymentRequiresServerSideRazorpayCredentials() {
        PaymentServiceImpl unconfigured = createService(unconfiguredProperties());

        when(orderClient.getOrderSummary(1L, 7L)).thenReturn(order("PENDING_PAYMENT"));
        when(paymentRepository.findByOrderIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        when(paymentRepository.findFirstByOrderIdAndStatusInOrderByCreatedAtDesc(
                1L, List.of(PaymentStatus.CREATED, PaymentStatus.PENDING)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> unconfigured.createPayment(BUYER, new CreatePaymentRequest(1L)))
                .isInstanceOf(PaymentConfigurationException.class)
                .hasMessageContaining("not configured");

        verifyNoInteractions(razorpayGateway);
    }

    @Test
    void createPaymentCreatesTheRazorpayOrderAndPersistsAPendingPayment() {
        Payment storedPayment = payment(PaymentStatus.CREATED);

        when(orderClient.getOrderSummary(1L, 7L)).thenReturn(order("PENDING_PAYMENT"));
        when(paymentRepository.findByOrderIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        when(paymentRepository.findFirstByOrderIdAndStatusInOrderByCreatedAtDesc(
                1L, List.of(PaymentStatus.CREATED, PaymentStatus.PENDING)))
                .thenReturn(Optional.empty());
        when(razorpayGateway.createOrder(
                new BigDecimal("59.98"), "order_1",
                Map.of("orderId", "1", "userId", "7")))
                .thenReturn(new RazorpayOrderResult(RAZORPAY_ORDER_ID,
                        new BigDecimal("59.98"), "INR"));
        when(persistenceService.createPayment(any(Payment.class))).thenReturn(storedPayment);

        PaymentOrderResponse response =
                service.createPayment(BUYER, new CreatePaymentRequest(1L));

        assertThat(response.orderId()).isEqualTo(1L);
        assertThat(response.razorpayOrderId()).isEqualTo(RAZORPAY_ORDER_ID);
        assertThat(response.razorpayKeyId()).isEqualTo("rzp_test_key");
        assertThat(response.currency()).isEqualTo("INR");

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(persistenceService).createPayment(captor.capture());

        Payment created = captor.getValue();
        assertThat(created.getOrderId()).isEqualTo(1L);
        assertThat(created.getUserId()).isEqualTo(7L);
        assertThat(created.getUserEmail()).isEqualTo("buyer@example.com");
        assertThat(created.getAmount()).isEqualByComparingTo("59.98");
        assertThat(created.getCurrency()).isEqualTo("INR");
        assertThat(created.getStatus()).isEqualTo(PaymentStatus.CREATED);
        assertThat(created.getRazorpayOrderId()).isEqualTo(RAZORPAY_ORDER_ID);
    }

    @Test
    void verifyPaymentRejectsUnknownRazorpayOrders() {
        when(paymentRepository.findByRazorpayOrderId(RAZORPAY_ORDER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verifyPayment(BUYER, verification()))
                .isInstanceOf(PaymentNotFoundException.class);

        verifyNoInteractions(signatureVerifier);
    }

    @Test
    void verifyPaymentHidesOtherBuyersPayments() {
        Payment payment = payment(PaymentStatus.CREATED);
        payment.setUserId(8L);

        when(paymentRepository.findByRazorpayOrderId(RAZORPAY_ORDER_ID))
                .thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> service.verifyPayment(BUYER, verification()))
                .isInstanceOf(PaymentAccessDeniedException.class);

        verifyNoInteractions(signatureVerifier);
    }

    @Test
    void verifyPaymentIsIdempotentForSuccessfulPayments() {
        Payment payment = payment(PaymentStatus.SUCCESS);
        payment.setRazorpayPaymentId("pay_XYZ23");

        when(paymentRepository.findByRazorpayOrderId(RAZORPAY_ORDER_ID))
                .thenReturn(Optional.of(payment));

        PaymentStatusResponse response =
                createService(unconfiguredProperties()).verifyPayment(BUYER, verification());

        assertThat(response.status()).isEqualTo("SUCCESS");
        assertThat(response.razorpayPaymentId()).isEqualTo("pay_XYZ23");
        verifyNoInteractions(signatureVerifier, settlementService);
    }

    @Test
    void verifyPaymentRequiresServerSideCredentials() {
        PaymentServiceImpl unconfigured = createService(unconfiguredProperties());

        when(paymentRepository.findByRazorpayOrderId(RAZORPAY_ORDER_ID))
                .thenReturn(Optional.of(payment(PaymentStatus.CREATED)));

        assertThatThrownBy(() -> unconfigured.verifyPayment(BUYER, verification()))
                .isInstanceOf(PaymentConfigurationException.class);

        verifyNoInteractions(signatureVerifier);
    }

    @Test
    void verifyPaymentRejectsAnInvalidSignature() {
        Payment payment = payment(PaymentStatus.CREATED);

        when(paymentRepository.findByRazorpayOrderId(RAZORPAY_ORDER_ID))
                .thenReturn(Optional.of(payment));
        when(signatureVerifier.verifyPayment(
                RAZORPAY_ORDER_ID, "pay_XYZ23", "signature", "test_secret"))
                .thenReturn(false);

        assertThatThrownBy(() -> service.verifyPayment(BUYER, verification()))
                .isInstanceOf(PaymentVerificationException.class)
                .hasMessageContaining("invalid");

        verify(settlementService, never()).markSuccessful(any(), any());
    }

    @Test
    void verifyPaymentSettlesOnlyAfterAValidSignature() {
        Payment payment = payment(PaymentStatus.CREATED);

        when(paymentRepository.findByRazorpayOrderId(RAZORPAY_ORDER_ID))
                .thenReturn(Optional.of(payment));
        when(signatureVerifier.verifyPayment(
                RAZORPAY_ORDER_ID, "pay_XYZ23", "signature", "test_secret"))
                .thenReturn(true);
        when(settlementService.markSuccessful(payment, "pay_XYZ23")).thenAnswer(invocation -> {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setRazorpayPaymentId("pay_XYZ23");
            return payment;
        });

        PaymentStatusResponse response = service.verifyPayment(BUYER, verification());

        assertThat(response.status()).isEqualTo("SUCCESS");
        assertThat(response.razorpayPaymentId()).isEqualTo("pay_XYZ23");
    }

    @Test
    void verifyPaymentSurfacesDuplicateRazorpayPaymentAsAStateConflict() {
        Payment payment = payment(PaymentStatus.CREATED);

        when(paymentRepository.findByRazorpayOrderId(RAZORPAY_ORDER_ID))
                .thenReturn(Optional.of(payment));
        when(signatureVerifier.verifyPayment(
                RAZORPAY_ORDER_ID, "pay_XYZ23", "signature", "test_secret"))
                .thenReturn(true);
        when(settlementService.markSuccessful(payment, "pay_XYZ23"))
                .thenThrow(new IllegalStateException("already linked"));

        assertThatThrownBy(() -> service.verifyPayment(BUYER, verification()))
                .isInstanceOf(PaymentStateException.class)
                .hasMessageContaining("already linked");
    }

    @Test
    void paymentStatusIsOnlyVisibleToItsOwner() {
        Payment payment = payment(PaymentStatus.SUCCESS);

        when(paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc(1L))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc(2L))
                .thenReturn(Optional.empty());

        assertThat(service.getPaymentForOrder(1L, BUYER).status()).isEqualTo("SUCCESS");

        JwtPrincipal other = new JwtPrincipal(8L, "other@example.com", List.of("USER"));
        assertThatThrownBy(() -> service.getPaymentForOrder(1L, other))
                .isInstanceOf(PaymentAccessDeniedException.class);

        assertThatThrownBy(() -> service.getPaymentForOrder(2L, BUYER))
                .isInstanceOf(PaymentNotFoundException.class);
    }

    @Test
    void webhookEndpointRequiresAConfiguredSecret() {
        PaymentServiceImpl unconfigured = createService(unconfiguredProperties());

        assertThatThrownBy(() -> unconfigured.handleWebhook("{}", "signature", "evt-1"))
                .isInstanceOf(PaymentConfigurationException.class)
                .hasMessageContaining("webhook secret");

        verifyNoInteractions(signatureVerifier, webhookParser);
    }

    @Test
    void webhookEndpointRejectsInvalidSignatures() {
        when(signatureVerifier.verifyWebhook("{}", "signature", "webhook_secret"))
                .thenReturn(false);

        assertThatThrownBy(() -> service.handleWebhook("{}", "signature", "evt-1"))
                .isInstanceOf(PaymentVerificationException.class);

        verifyNoInteractions(webhookParser, webhookHandler);
    }

    @Test
    void verifiedWebhookIsParsedAndDelegated() {
        RazorpayWebhookEvent event = new RazorpayWebhookEvent(
                "payment.captured", RAZORPAY_ORDER_ID, "pay_XYZ23", null);

        when(signatureVerifier.verifyWebhook("{}", "signature", "webhook_secret"))
                .thenReturn(true);
        when(webhookParser.parse("{}")).thenReturn(event);

        service.handleWebhook("{}", "signature", "evt-1");

        verify(webhookHandler).handle("evt-1", event);
    }

    private PaymentServiceImpl createService(RazorpayProperties properties) {
        return new PaymentServiceImpl(paymentRepository, persistenceService, settlementService,
                webhookHandler, orderClient, razorpayGateway, signatureVerifier,
                webhookParser, properties);
    }

    private RazorpayProperties configuredProperties() {
        return new RazorpayProperties("rzp_test_key", "test_secret", "webhook_secret", "INR");
    }

    private RazorpayProperties unconfiguredProperties() {
        return new RazorpayProperties("", "", "", "INR");
    }

    private OrderSummary order(String status) {
        return new OrderSummary(1L, 7L, "buyer@example.com",
                new BigDecimal("59.98"), status);
    }

    private Payment payment(PaymentStatus status) {
        Payment payment = new Payment();
        payment.setOrderId(1L);
        payment.setUserId(7L);
        payment.setUserEmail("buyer@example.com");
        payment.setAmount(new BigDecimal("59.98"));
        payment.setCurrency("INR");
        payment.setStatus(status);
        payment.setRazorpayOrderId(RAZORPAY_ORDER_ID);
        return payment;
    }

    private VerifyPaymentRequest verification() {
        return new VerifyPaymentRequest(RAZORPAY_ORDER_ID, "pay_XYZ23", "signature");
    }
}
