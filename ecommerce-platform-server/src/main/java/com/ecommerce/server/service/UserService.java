package com.ecommerce.server.service;

import com.ecommerce.server.entity.User;

public interface UserService {

    User register(User user);

    User findByEmail(String email);

    User login(String email, String password);
}