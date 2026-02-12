package com.example.plateit.responses;

public class AuthResponse {
    private String user_id;
    private String email;
    private String username;
    private String full_name;
    private String message;

    public String getUserId() {
        return user_id;
    }

    public String getEmail() {
        return email;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return full_name;
    }

    public String getMessage() {
        return message;
    }
}
