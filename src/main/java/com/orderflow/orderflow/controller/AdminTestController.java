package com.orderflow.orderflow.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/test")
public class AdminTestController {

    @GetMapping
    public Map<String, String> test() {
        return Map.of("message", "Admin access granted");
    }
}
