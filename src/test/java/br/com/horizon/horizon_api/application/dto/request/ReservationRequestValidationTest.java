package br.com.horizon.horizon_api.application.dto.request;

import br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.TripType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
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
    @DisplayName("Deve falhar quando passageiro tiver idade negativa ou nome em branco")
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
    @DisplayName("Deve falhar quando passageiro tiver idade superior a 120 anos")
    void shouldFailWhenPassengerHasAgeAbove120() {
        ReservationRequest request = new ReservationRequest();
        request.setDestinationId(1L);
        request.setTripType(TripType.ONE_WAY);
        request.setOutboundFlightId(10L);

        PassengerRequest invalidPassenger = new PassengerRequest();
        invalidPassenger.setName("Idoso Centenário");
        invalidPassenger.setAge(121); // Excede o limite máximo de 120 anos

        request.setPassengers(List.of(invalidPassenger));

        Set<ConstraintViolation<ReservationRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Deveria acusar violação de idade máxima");
        boolean hasAgeViolation = violations.stream().anyMatch(v -> v.getPropertyPath().toString().contains("passengers[0].age"));
        assertTrue(hasAgeViolation, "Deveria violar @Max(120)");
    }

    @Test
    @DisplayName("Deve passar quando passageiro tiver idade exatamente 120 anos")
    void shouldPassWhenPassengerHasAgeExactly120() {
        ReservationRequest request = new ReservationRequest();
        request.setDestinationId(1L);
        request.setTripType(TripType.ONE_WAY);
        request.setOutboundFlightId(10L);

        PassengerRequest validPassenger = new PassengerRequest();
        validPassenger.setName("Idoso 120 Anos");
        validPassenger.setAge(120);

        request.setPassengers(List.of(validPassenger));

        Set<ConstraintViolation<ReservationRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Idade 120 anos é válida e deve passar");
    }

    @Test
    @DisplayName("Deve passar quando todos os dados da reserva forem válidos")
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
