package br.com.horizon.horizon_api.application.dto.response;

import br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.TripType;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.ReservationStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
public class ReservationResponse {
    private Long reservationId;
    private Long destinationId;
    private String destinationName;
    private TripType tripType;
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd")
    @io.swagger.v3.oas.annotations.media.Schema(example = "2024-12-31", type = "string", format = "date")
    private LocalDate departureDate;

    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd")
    @io.swagger.v3.oas.annotations.media.Schema(example = "2025-01-05", type = "string", format = "date")
    private LocalDate returnDate;
    private BigDecimal totalAmount;
    
    @io.swagger.v3.oas.annotations.media.Schema(implementation = ReservationStatus.class)
    private ReservationStatus status;
    private OffsetDateTime paymentDeadline;
    private OffsetDateTime createdAt;
    private OffsetDateTime cancelledAt;

    private FlightResponse outboundFlight;
    private FlightResponse returnFlight;
    private List<PassengerResponse> passengers;
    private List<TicketResponse> tickets;
}
