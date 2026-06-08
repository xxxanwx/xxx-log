package com.xxxlog.server.dto;

public class LoginResponse {

    private String token;
    private String username;
    private int expireHours;

    public LoginResponse() {
    }

    public LoginResponse(String token, String username, int expireHours) {
        this.token = token;
        this.username = username;
        this.expireHours = expireHours;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getExpireHours() {
        return expireHours;
    }

    public void setExpireHours(int expireHours) {
        this.expireHours = expireHours;
    }
}
