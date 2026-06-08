package com.xxxlog.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "xxx-log.auth")
public class AuthProperties {

    private String username = "admin";
    private String password = "admin";
    private int tokenExpireHours = 24;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getTokenExpireHours() {
        return tokenExpireHours;
    }

    public void setTokenExpireHours(int tokenExpireHours) {
        this.tokenExpireHours = tokenExpireHours;
    }
}
