package com.xxxlog.server.controller;

import com.xxxlog.server.config.AuthProperties;
import com.xxxlog.server.dto.ApiResponse;
import com.xxxlog.server.dto.LoginRequest;
import com.xxxlog.server.dto.LoginResponse;
import com.xxxlog.server.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthProperties authProperties;

    public AuthController(AuthService authService, AuthProperties authProperties) {
        this.authService = authService;
        this.authProperties = authProperties;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {
        try {
            String token = authService.login(request.getUsername(), request.getPassword());
            return ApiResponse.ok(new LoginResponse(
                    token,
                    request.getUsername(),
                    authProperties.getTokenExpireHours()));
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(401, e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        authService.logout(extractToken(request));
        return ApiResponse.ok(null);
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, String>> me(HttpServletRequest request) {
        String token = extractToken(request);
        String username = authService.getUsername(token);
        if (username == null) {
            return ApiResponse.fail(401, "未登录或登录已过期");
        }
        Map<String, String> data = new HashMap<>();
        data.put("username", username);
        return ApiResponse.ok(data);
    }

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return request.getHeader("X-Auth-Token");
    }
}
