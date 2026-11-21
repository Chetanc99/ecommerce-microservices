package com.ecommerce.server.service;

import com.ecommerce.server.entity.User;
import com.ecommerce.server.exception.DuplicateEmailException;
import com.ecommerce.server.exception.InvalidPasswordException;
import com.ecommerce.server.exception.UserNotFoundException;
import com.ecommerce.server.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private static final String DEFAULT_ROLE = "USER";
    private static final String ADMIN_ROLE = "ADMIN";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User register(User user) {

        String email = normalizeEmail(user.getEmail());

        if (userRepository.findByEmail(email).isPresent()) {
            throw new DuplicateEmailException();
        }

        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole(DEFAULT_ROLE);

        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public User login(String email, String password) {

        User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        return user;
    }

    @Override
    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmail(normalizeEmail(email)).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    @Override
    public User updateProfile(Long userId, String name, String email) {

        User user = findById(userId);
        String normalizedEmail = normalizeEmail(email);

        userRepository.findByEmail(normalizedEmail)
                .filter(existing -> !existing.getId().equals(userId))
                .ifPresent(existing -> {
                    throw new DuplicateEmailException();
                });

        user.setName(name.trim());
        user.setEmail(normalizedEmail);

        return userRepository.save(user);
    }

    @Override
    public void changePassword(Long userId, String currentPassword, String newPassword) {

        User user = findById(userId);

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new InvalidPasswordException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    public void ensureAdmin(String email, String password) {

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return;
        }

        String normalizedEmail = normalizeEmail(email);
        User admin = userRepository.findByEmail(normalizedEmail).orElse(null);

        if (admin == null) {
            admin = new User(
                    "Administrator",
                    normalizedEmail,
                    passwordEncoder.encode(password),
                    ADMIN_ROLE
            );
        } else {
            admin.setRole(ADMIN_ROLE);
        }

        userRepository.save(admin);
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
