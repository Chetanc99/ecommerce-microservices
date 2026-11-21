package com.ecommerce.server.service;

import com.ecommerce.server.entity.User;

public interface UserService {

    User register(User user);

    User login(String email, String password);

    User findByEmail(String email);

    User findById(Long id);

    User updateProfile(Long userId, String name, String email);

    void changePassword(Long userId, String currentPassword, String newPassword);

    void ensureAdmin(String email, String password);
}
