package com.ebookmarketplace.backend.service;

import com.ebookmarketplace.backend.dto.CurrentUserResponse;
import com.ebookmarketplace.backend.entity.User;
import com.ebookmarketplace.backend.repository.UserRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse getCurrentUser(Jwt jwt) {
        User user = getRequiredUser(jwt);

        return new CurrentUserResponse(
                user.getId(),
                user.getEmail(),
                user.getRole().name());
    }

    @Transactional(readOnly = true)
    public User getRequiredUser(Jwt jwt) {
        String email = jwt.getSubject();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        if (!user.isEnabled()) {
            throw new IllegalStateException("User account is disabled");
        }

        return user;
    }
}