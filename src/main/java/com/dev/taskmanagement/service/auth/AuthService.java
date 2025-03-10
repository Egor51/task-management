package com.dev.taskmanagement.service.auth;

import com.dev.taskmanagement.dto.auth.AuthResponse;
import com.dev.taskmanagement.dto.auth.LoginRequest;
import com.dev.taskmanagement.dto.auth.RegisterRequest;
import com.dev.taskmanagement.model.User;
import com.dev.taskmanagement.service.UserService;
import com.dev.taskmanagement.service.auth.JWTService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final JWTService jwtService;
    private final UserService userService;
    public AuthResponse register(RegisterRequest request) {
        User user = userService.createUser(request);
        String token = jwtService.generateToken(user);
        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        User user = userService.getUserByEmail(request.getEmail());
        String token = jwtService.generateToken(user);
        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .build();
    }
}
