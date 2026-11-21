import { request } from './client.js';

export function login(email, password) {
  return request('/users/login', {
    method: 'POST',
    auth: false,
    body: { email, password }
  });
}

export function register({ name, email, password }) {
  return request('/users/register', {
    method: 'POST',
    auth: false,
    body: { name, email, password }
  });
}

export function fetchCurrentUser() {
  return request('/users/me');
}

export function updateProfile({ name, email }) {
  return request('/users/me', {
    method: 'PUT',
    body: { name, email }
  });
}

export function changePassword({ currentPassword, newPassword }) {
  return request('/users/me/password', {
    method: 'PUT',
    body: { currentPassword, newPassword }
  });
}
