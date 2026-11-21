import { createPayment, verifyPayment } from '../api/payments.js';
import { openRazorpay } from './razorpay.js';

export async function payForOrder({
  orderId,
  amount,
  currency = 'INR',
  user,
  onVerified,
  onDismiss,
  onPaymentFailed,
  onFailure
}) {
  const payment = await createPayment(orderId);

  await openRazorpay({
    keyId: payment.razorpayKeyId,
    razorpayOrderId: payment.razorpayOrderId,
    amount: payment.amount ?? amount,
    currency: payment.currency || currency,
    name: user?.name,
    email: user?.email,
    onSuccess: async (result) => {
      try {
        const status = await verifyPayment({
          razorpayOrderId: result.razorpay_order_id,
          razorpayPaymentId: result.razorpay_payment_id,
          razorpaySignature: result.razorpay_signature
        });

        onVerified?.(status);
      } catch (exception) {
        onFailure?.(exception);
      }
    },
    onDismiss,
    onPaymentFailed
  });

  return payment;
}
