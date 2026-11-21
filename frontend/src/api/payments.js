import { request } from './client.js';

export function createPayment(orderId) {
  return request('/payments/orders', {
    method: 'POST',
    body: { orderId }
  });
}

export function verifyPayment({ razorpayOrderId, razorpayPaymentId, razorpaySignature }) {
  return request('/payments/verify', {
    method: 'POST',
    body: { razorpayOrderId, razorpayPaymentId, razorpaySignature }
  });
}

export function getPaymentForOrder(orderId) {
  return request(`/payments/order/${orderId}`);
}
