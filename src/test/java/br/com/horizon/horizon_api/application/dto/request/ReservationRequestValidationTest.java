package br.com.horizon.horizon_api.application.dto.request;

import br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.TripType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReservationRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void shouldFailWhenPassengerHasInvalidAgeOrBlankName() {
        ReservationRequest request = new ReservationRequest();
        request.setDestinationId(1L);
        request.setTripType(TripType.ONE_WAY);
        request.setOutboundFlightId(10L);

        PassengerRequest invalidPassenger = new PassengerRequest();
        invalidPassenger.setName(""); // Invalid: blank
        invalidPassenger.setAge(-5); // Invalid: min is 1

        request.setPassengers(List.of(invalidPassenger));

        Set<ConstraintViolation<ReservationRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty(), "Violations should be reported for invalid nested passenger");
        boolean hasNameViolation = violations.stream().anyMatch(v -> v.getPropertyPath().toString().contains("passengers[0].name"));
        boolean hasAgeViolation = violations.stream().anyMatch(v -> v.getPropertyPath().toString().contains("passengers[0].age"));

        assertTrue(hasNameViolation, "Should violate passenger name constraint");
        assertTrue(hasAgeViolation, "Should violate passenger age constraint");
    }

    @Test
    void shouldPassWhenAllDataIsValid() {
        ReservationRequest request = new ReservationRequest();
        request.setDestinationId(1L);
        request.setTripType(TripType.ONE_WAY);
        request.setOutboundFlightId(10L);

        PassengerRequest validPassenger = new PassengerRequest();
        validPassenger.setName("Carlos Silva");
        validPassenger.setAge(30);

        request.setPassengers(List.of(validPassenger));

        Set<ConstraintViolation<ReservationRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Should have no violations for valid reservation");
    }
}
