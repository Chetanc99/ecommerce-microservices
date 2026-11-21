package com.ecommerce.payment.razorpay;

import java.math.BigDecimal;
import java.util.Map;

public interface RazorpayGateway {

    RazorpayOrderResult createOrder(BigDecimal amount, String receipt, Map<String, String> notes);
}
