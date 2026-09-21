package br.com.horizon.horizon_api.domain.model;

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
public class Reservation {
    private Long id;
    private Long userId;
    private Long destinationId;
    private Long outboundFlightId;
    private Long returnFlightId;
    private TripType tripType;
    private LocalDate departureDate;
    private LocalDate returnDate;
    private Integer numberOfPassengers;
    private BigDecimal totalAmount;
    private ReservationStatus status;
    private OffsetDateTime createdAt;
    private OffsetDateTime paymentDeadline;
    private OffsetDateTime cancelledAt;
    
    private List<Passenger> passengers;
    private Payment payment;
}
