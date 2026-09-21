package br.com.horizon.horizon_api.domain.model;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class Flight {
    private Long id;
    private Long destinationId;
    private String originAirportCode;
    private String destinationAirportCode;
    private LocalDate flightDate;
    private LocalTime departureTime;
    private LocalTime arrivalTime;
    private String flightNumber;
    private BigDecimal pricePerPerson;
    private BigDecimal priceExecutive;
    private BigDecimal pricePremium;
    private Boolean active;
}
