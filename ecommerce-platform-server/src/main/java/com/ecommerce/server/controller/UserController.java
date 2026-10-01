package com.ecommerce.server.controller;

import com.ecommerce.server.entity.User;
import com.ecommerce.server.dto.UserResponse;
import com.ecommerce.server.service.UserService;
import org.springframework.http.ResponseEntity;
import com.ecommerce.server.dto.LoginRequest;
import com.ecommerce.server.dto.LoginResponse;
import com.ecommerce.server.security.JwtService;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

	private final UserService userService;
	private final JwtService jwtService;

	public UserController(UserService userService, JwtService jwtService) {
	    this.userService = userService;
	    this.jwtService = jwtService;
	}

    
	@PostMapping("/login")
	public ResponseEntity<LoginResponse> login(
	        @RequestBody LoginRequest request) {

	    User user = userService.login(
	            request.getEmail(),
	            request.getPassword()
	    );

	    if (user == null) {
	        return ResponseEntity
	                .status(HttpStatus.UNAUTHORIZED)
	                .build();
	    }

	    // JWT token generate
	    String token = jwtService.generateToken(user.getEmail());

	    LoginResponse response = new LoginResponse(
	            token,
	            user.getId(),
	            user.getName(),
	            user.getEmail(),
	            user.getRole()
	    );

	    return ResponseEntity.ok(response);
	}
	
	
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@RequestBody User user) {

        User savedUser = userService.register(user);

        UserResponse response = new UserResponse(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getRole()
        );

        return ResponseEntity.ok(response);
    }
   
}
