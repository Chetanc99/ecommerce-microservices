import { buildQuery, request } from './client.js';

export function createOrder(items) {
  return request('/orders', {
    method: 'POST',
    body: { items: items.map(({ productId, quantity }) => ({ productId, quantity })) }
  });
}

export function listOrders({ page = 0, size = 10, sort = 'createdAt,desc' } = {}) {
  return request(`/orders${buildQuery({ page, size, sort })}`);
}

export function getOrder(orderId) {
  return request(`/orders/${orderId}`);
}

export function cancelOrder(orderId) {
  return request(`/orders/${orderId}/cancel`, { method: 'PUT' });
}
