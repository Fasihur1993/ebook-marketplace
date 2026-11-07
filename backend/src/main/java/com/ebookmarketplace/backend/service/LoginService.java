package com.ebookmarketplace.backend.service;

import com.ebookmarketplace.backend.dto.LoginRequest;
import com.ebookmarketplace.backend.dto.LoginResponse;
import com.ebookmarketplace.backend.entity.User;
import com.ebookmarketplace.backend.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public LoginService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {

        String email = request.email().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new BadCredentialsException("Invalid email or password")
                );

        if (!user.isEnabled()
                || !passwordEncoder.matches(
                        request.password(),
                        user.getPasswordHash()
                )) {
            throw new BadCredentialsException(
                    "Invalid email or password"
            );
        }

        String accessToken = jwtTokenService.generateToken(user);

        return new LoginResponse(
                accessToken,
                "Bearer",
                jwtTokenService.getExpirationSeconds()
        );
    }
}