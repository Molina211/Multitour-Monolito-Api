package com.corhuila.errorcapa8.travesia_natural.tenants.domain.model;

import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.InvalidTenantException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordPolicyTest {

    @Test
    void acceptsAPasswordMeetingAllRequirements() {
        assertThatCode(() -> PasswordPolicy.validate("Abcdef1$"))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsNullPassword() {
        assertThatThrownBy(() -> PasswordPolicy.validate(null))
                .isInstanceOf(InvalidTenantException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Abc1$",       // too short
            "abcdefg1$",   // no uppercase
            "ABCDEFG1$",   // no lowercase
            "Abcdefgh$",   // no digit
            "Abcdefg12",   // no special character
    })
    void rejectsPasswordsMissingARequirement(String invalidPassword) {
        assertThatThrownBy(() -> PasswordPolicy.validate(invalidPassword))
                .isInstanceOf(InvalidTenantException.class)
                .hasMessageContaining("password must be at least 8 characters");
    }
}
