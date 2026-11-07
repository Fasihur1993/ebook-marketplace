package com.ebookmarketplace.backend.controller;

import com.ebookmarketplace.backend.dto.ApiResponse;
import com.ebookmarketplace.backend.dto.LoginRequest;
import com.ebookmarketplace.backend.dto.LoginResponse;
import com.ebookmarketplace.backend.dto.RegisterRequest;
import com.ebookmarketplace.backend.dto.RegisterResponse;
import com.ebookmarketplace.backend.service.LoginService;
import com.ebookmarketplace.backend.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ebookmarketplace.backend.dto.CurrentUserResponse;
import com.ebookmarketplace.backend.service.CurrentUserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final RegistrationService registrationService;
    private final LoginService loginService;
    private final CurrentUserService currentUserService;

    public AuthController(
            RegistrationService registrationService,
            LoginService loginService,
            CurrentUserService currentUserService
    ) {
        this.registrationService = registrationService;
        this.loginService = loginService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(
            @Valid @RequestBody RegisterRequest request
    ) {

        RegisterResponse response =
                registrationService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Registration successful",
                        response
                ));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {

        LoginResponse response = loginService.login(request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Login successful",
                        response
                )
        );
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<CurrentUserResponse>> getCurrentUser(
            @AuthenticationPrincipal Jwt jwt
    ) {

        CurrentUserResponse response =
                currentUserService.getCurrentUser(jwt);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "User details retrieved successfully",
                        response
                )
        );
    }
}