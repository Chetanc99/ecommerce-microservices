package com.ecommerce.server.controller;

import com.ecommerce.common.security.JwtPrincipal;
import com.ecommerce.common.security.JwtService;
import com.ecommerce.server.dto.ChangePasswordRequest;
import com.ecommerce.server.dto.LoginRequest;
import com.ecommerce.server.dto.LoginResponse;
import com.ecommerce.server.dto.RegisterRequest;
import com.ecommerce.server.dto.UpdateProfileRequest;
import com.ecommerce.server.dto.UserResponse;
import com.ecommerce.server.entity.User;
import com.ecommerce.server.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final JwtService jwtService;

    public UserController(UserService userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        User user = new User(
                request.getName(),
                request.getEmail(),
                request.getPassword(),
                "USER"
        );

        User savedUser = userService.register(user);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(toResponse(savedUser));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {

        User user = userService.login(request.getEmail(), request.getPassword());

        String token = jwtService.generateToken(
                user.getId(),
                user.getEmail(),
                List.of(user.getRole())
        );

        return ResponseEntity.ok(new LoginResponse(
                token,
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole()
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> currentUser(
            @AuthenticationPrincipal JwtPrincipal principal) {

        return ResponseEntity.ok(toResponse(userService.findById(principal.userId())));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateProfile(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request) {

        User user = userService.updateProfile(
                principal.userId(),
                request.getName(),
                request.getEmail()
        );

        return ResponseEntity.ok(toResponse(user));
    }

    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request) {

        userService.changePassword(
                principal.userId(),
                request.getCurrentPassword(),
                request.getNewPassword()
        );

        return ResponseEntity.noContent().build();
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole()
        );
    }
}
