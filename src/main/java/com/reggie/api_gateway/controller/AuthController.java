package com.reggie.api_gateway.controller;

import com.reggie.api_gateway.dto.IngestRequest;
import com.reggie.api_gateway.dto.LoginRequest;
import com.reggie.api_gateway.security.JwtService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {
    private final JwtService service;

    public AuthController(JwtService service) {
        this.service = service;
    }

    @PostMapping("/login")
    public String login(@RequestBody LoginRequest request) {
        return service.generateToken(request.getUsername());
    }
}
