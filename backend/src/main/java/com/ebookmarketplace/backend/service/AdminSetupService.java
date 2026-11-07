package com.ebookmarketplace.backend.service;

import com.ebookmarketplace.backend.dto.AdminSetupRequest;
import com.ebookmarketplace.backend.dto.RegisterResponse;
import com.ebookmarketplace.backend.entity.Role;
import com.ebookmarketplace.backend.entity.User;
import com.ebookmarketplace.backend.exception.EmailAlreadyExistsException;
import com.ebookmarketplace.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ebookmarketplace.backend.exception.InvalidAdminSetupKeyException;
import com.ebookmarketplace.backend.exception.AdminSetupAlreadyCompletedException;

@Service
public class AdminSetupService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String setupKey;

    public AdminSetupService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${security.admin.setup-key}") String setupKey
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.setupKey = setupKey;
    }

    @Transactional
    public RegisterResponse createInitialAdmin(AdminSetupRequest request) {

        if (!setupKey.equals(request.setupKey())) {
            throw new InvalidAdminSetupKeyException(
                    "Invalid administrator setup key"
            );
        }

        if (userRepository.existsByRole(Role.ADMIN)) {
            throw new AdminSetupAlreadyCompletedException(
                    "Administrator setup has already been completed"
             );
        }

        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(
                    "An account with this email already exists"
            );
        }

        User admin = new User(
                email,
                passwordEncoder.encode(request.password()),
                request.fullName().trim(),
                Role.ADMIN
        );

        User savedAdmin = userRepository.save(admin);

        return new RegisterResponse(
                savedAdmin.getId(),
                savedAdmin.getFullName(),
                savedAdmin.getEmail(),
                savedAdmin.getRole().name()
        );
    }
}