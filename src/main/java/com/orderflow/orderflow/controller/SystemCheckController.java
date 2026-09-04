package com.orderflow.orderflow.controller;

import com.orderflow.orderflow.entity.SystemCheck;
import com.orderflow.orderflow.repository.SystemCheckRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/system-check")
public class SystemCheckController {

    private final SystemCheckRepository systemCheckRepository;

    public SystemCheckController(SystemCheckRepository systemCheckRepository) {
        this.systemCheckRepository = systemCheckRepository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SystemCheck create(@Valid @RequestBody CreateSystemCheckRequest request) {
        return systemCheckRepository.save(new SystemCheck(request.message().trim()));
    }

    @GetMapping
    public List<SystemCheck> findAll() {
        return systemCheckRepository.findAll();
    }

    public record CreateSystemCheckRequest(
            @NotBlank(message = "message is required")
            @Size(max = 255, message = "message must be at most 255 characters")
            String message) {
    }
}
