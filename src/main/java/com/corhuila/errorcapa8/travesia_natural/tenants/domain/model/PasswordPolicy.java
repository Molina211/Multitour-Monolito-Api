package com.corhuila.errorcapa8.travesia_natural.tenants.domain.model;

import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.InvalidTenantException;

import java.util.regex.Pattern;

/**
 * Enforces the password policy already described (but not validated) by the Frontend
 * signup screen (signup.component.html): minimum 8 characters, at least one uppercase,
 * one lowercase, one digit and one special character.
 */
public final class PasswordPolicy {

    private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern DIGIT = Pattern.compile("\\d");
    private static final Pattern SPECIAL_CHARACTER = Pattern.compile("[^A-Za-z0-9]");

    private PasswordPolicy() {
    }

    public static void validate(String password) {
        if (password == null
                || password.length() < 8
                || !UPPERCASE.matcher(password).find()
                || !LOWERCASE.matcher(password).find()
                || !DIGIT.matcher(password).find()
                || !SPECIAL_CHARACTER.matcher(password).find()) {
            throw new InvalidTenantException(
                    "password must be at least 8 characters and include an uppercase letter, "
                            + "a lowercase letter, a number and a special character");
        }
    }
}
