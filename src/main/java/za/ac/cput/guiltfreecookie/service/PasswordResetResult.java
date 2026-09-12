package za.ac.cput.guiltfreecookie.service;

import za.ac.cput.guiltfreecookie.domain.Admin;

public class PasswordResetResult {

    private final Admin admin;
    private final String temporaryPassword;

    public PasswordResetResult(Admin admin, String temporaryPassword) {
        this.admin = admin;
        this.temporaryPassword = temporaryPassword;
    }

    public Admin getAdmin() {
        return admin;
    }

    public String getTemporaryPassword() {
        return temporaryPassword;
    }
}
