package br.com.horizon.horizon_api.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "flights")
@Getter
@Setter
public class FlightEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_id", nullable = false)
    private DestinationEntity destination;

    @Column(name = "origin_airport_code", length = 3)
    private String originAirportCode;

    @Column(name = "destination_airport_code", length = 3)
    private String destinationAirportCode;

    @Column(name = "flight_date")
    private LocalDate flightDate;

    @Column(name = "departure_time")
    private LocalTime departureTime;

    @Column(name = "arrival_time")
    private LocalTime arrivalTime;

    @Column(name = "flight_number", length = 20)
    private String flightNumber;

    @Column(name = "price_per_person", precision = 10, scale = 2)
    private BigDecimal pricePerPerson;

    @Column(name = "price_executive", precision = 10, scale = 2)
    private BigDecimal priceExecutive;

    @Column(name = "price_premium", precision = 10, scale = 2)
    private BigDecimal pricePremium;

    @Column(name = "active")
    private Boolean active;
}
