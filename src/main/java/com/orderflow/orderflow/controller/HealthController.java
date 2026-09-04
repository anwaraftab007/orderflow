package com.orderflow.orderflow.controller;

import com.orderflow.orderflow.repository.SystemCheckRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final SystemCheckRepository systemCheckRepository;

    public HealthController(SystemCheckRepository systemCheckRepository) {
        this.systemCheckRepository = systemCheckRepository;
    }

    @GetMapping
    public ResponseEntity<Map<String, String>> health() {
        try {
            systemCheckRepository.count();
            return ResponseEntity.ok(Map.of("status", "UP", "database", "CONNECTED"));
        } catch (DataAccessException exception) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("status", "DOWN", "database", "UNAVAILABLE"));
        }
    }
}
