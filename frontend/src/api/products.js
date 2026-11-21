import { buildQuery, request } from './client.js';

export function listProducts({ page = 0, size = 12, sort = 'createdAt,desc' } = {}) {
  return request(`/products${buildQuery({ page, size, sort })}`, { auth: false });
}

export function listProductsByCategory(category, { page = 0, size = 12, sort = 'createdAt,desc' } = {}) {
  return request(
    `/products/category/${encodeURIComponent(category)}${buildQuery({ page, size, sort })}`,
    { auth: false }
  );
}

export function searchProducts(name, { page = 0, size = 12, sort = 'createdAt,desc' } = {}) {
  return request(`/products/search${buildQuery({ name, page, size, sort })}`, { auth: false });
}

export function getProduct(id) {
  return request(`/products/${id}`, { auth: false });
}

export function createProduct(payload) {
  return request('/products', { method: 'POST', body: payload });
}

export function updateProduct(id, payload) {
  return request(`/products/${id}`, { method: 'PUT', body: payload });
}

export function deleteProduct(id) {
  return request(`/products/${id}`, { method: 'DELETE' });
}
