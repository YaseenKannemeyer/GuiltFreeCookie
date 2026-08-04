package za.ac.cput.guiltfreecookie.factory;

import za.ac.cput.guiltfreecookie.domain.Admin;
import za.ac.cput.guiltfreecookie.util.Helper;

public class AdminFactory {

    public static Admin createAdmin(
            String adminId,
            String firstName,
            String lastName,
            String email,
            String password) {

        if (Helper.isNullOrEmpty(firstName)
                || Helper.isNullOrEmpty(lastName)
                || Helper.isNullOrEmpty(email)
                || Helper.isNullOrEmpty(password)) {
            return null;
        }

        return new Admin.Builder()
                .setAdminId(adminId)
                .setFirstName(firstName)
                .setLastName(lastName)
                .setEmail(email)
                .setPassword(password)
                .build();
    }
}