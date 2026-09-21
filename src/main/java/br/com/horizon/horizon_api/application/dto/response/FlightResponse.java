package br.com.horizon.horizon_api.application.dto.response;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class FlightResponse {
    private Long id;
    private Long destinationId;
    private String originAirportCode;
    private String destinationAirportCode;
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd")
    @io.swagger.v3.oas.annotations.media.Schema(example = "2024-12-31", type = "string", format = "date")
    private LocalDate flightDate;
    private LocalTime departureTime;
    private LocalTime arrivalTime;
    private String flightNumber;
    private BigDecimal pricePerPerson;
    private BigDecimal priceExecutive;
    private BigDecimal pricePremium;
    private Integer totalSeats;
    private Integer availableSeats;
}
