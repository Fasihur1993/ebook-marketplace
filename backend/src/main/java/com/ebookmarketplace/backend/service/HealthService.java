package com.ebookmarketplace.backend.service;

import com.ebookmarketplace.backend.dto.HealthResponse;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class HealthService {

    private final JdbcTemplate jdbcTemplate;

    public HealthService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public HealthResponse checkHealth() {

        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);

            return new HealthResponse("UP", "UP");

        } catch (DataAccessException exception) {

            return new HealthResponse("DOWN", "DOWN");
        }
    }
}