package br.com.horizon.horizon_api.application.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResetPasswordValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void shouldRejectPasswordLessThanSixCharacters() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("valid-uuid-token");
        request.setNewPassword("12345"); // Only 5 chars

        Set<ConstraintViolation<ResetPasswordRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Should reject passwords shorter than 6 characters");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("newPassword")));
    }

    @Test
    void shouldAcceptValidPassword() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("valid-uuid-token");
        request.setNewPassword("securePass123");

        Set<ConstraintViolation<ResetPasswordRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Should accept valid password");
    }
}
