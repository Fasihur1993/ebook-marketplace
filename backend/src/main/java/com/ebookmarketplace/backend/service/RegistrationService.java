package com.ebookmarketplace.backend.service;

import com.ebookmarketplace.backend.dto.RegisterRequest;
import com.ebookmarketplace.backend.dto.RegisterResponse;
import com.ebookmarketplace.backend.entity.Role;
import com.ebookmarketplace.backend.entity.User;
import com.ebookmarketplace.backend.exception.EmailAlreadyExistsException;
import com.ebookmarketplace.backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistrationService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {

        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(
                    "An account with this email already exists"
            );
        }

        String passwordHash =
                passwordEncoder.encode(request.password());

        User user = new User(
                email,
                passwordHash,
                request.fullName().trim(),
                Role.CUSTOMER
        );

        User savedUser = userRepository.save(user);

        return new RegisterResponse(
                savedUser.getId(),
                savedUser.getFullName(),
                savedUser.getEmail(),
                savedUser.getRole().name()
        );
    }
}