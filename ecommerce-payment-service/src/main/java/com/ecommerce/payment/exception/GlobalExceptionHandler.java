package com.ecommerce.payment.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(PaymentNotFoundException.class)
    public ProblemDetail handleNotFound(PaymentNotFoundException exception) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND,
                exception.getMessage());
        problem.setTitle("Payment not found");
        return problem;
    }

    @ExceptionHandler(PaymentAccessDeniedException.class)
    public ProblemDetail handleAccessDenied(PaymentAccessDeniedException exception) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN,
                exception.getMessage());
        problem.setTitle("Payment access denied");
        return problem;
    }

    @ExceptionHandler(PaymentStateException.class)
    public ProblemDetail handleState(PaymentStateException exception) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                exception.getMessage());
        problem.setTitle("Payment cannot be created");
        return problem;
    }

    @ExceptionHandler(PaymentVerificationException.class)
    public ProblemDetail handleVerification(PaymentVerificationException exception) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                exception.getMessage());
        problem.setTitle("Payment verification failed");
        return problem;
    }

    @ExceptionHandler(PaymentConfigurationException.class)
    public ProblemDetail handleConfiguration(PaymentConfigurationException exception) {

        log.error("Payment configuration problem: {}", exception.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "Payment processing is not configured on the server.");
        problem.setTitle("Payment unavailable");
        return problem;
    }

    @ExceptionHandler(OrderServiceUnavailableException.class)
    public ProblemDetail handleOrderService(OrderServiceUnavailableException exception) {

        log.error("Order service call failed", exception);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "The order service is temporarily unavailable. Please retry.");
        problem.setTitle("Order service unavailable");
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException exception) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "The request contains invalid fields.");
        problem.setTitle("Validation failed");

        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));

        problem.setProperty("errors", fieldErrors);
        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadableMessage(HttpMessageNotReadableException exception) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "The request body is missing or malformed.");
        problem.setTitle("Malformed request");
        return problem;
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalState(IllegalStateException exception) {

        log.error("Payment state conflict", exception);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                exception.getMessage());
        problem.setTitle("Payment state conflict");
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception exception) {

        log.error("Unhandled payment service error", exception);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "The payment request could not be completed.");
        problem.setTitle("Internal server error");
        return problem;
    }
}
