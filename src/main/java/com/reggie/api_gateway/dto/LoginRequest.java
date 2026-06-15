package com.reggie.api_gateway.dto;

public class LoginRequest {
    private String username;
    private String password;

    public LoginRequest() {

    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
