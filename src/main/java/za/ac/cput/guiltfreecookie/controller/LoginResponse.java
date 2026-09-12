package za.ac.cput.guiltfreecookie.controller;

import za.ac.cput.guiltfreecookie.domain.Admin;

public class LoginResponse {

    private final Admin admin;
    private final String token;

    public LoginResponse(Admin admin, String token) {
        this.admin = admin;
        this.token = token;
    }

    public Admin getAdmin() {
        return admin;
    }

    public String getToken() {
        return token;
    }
}
