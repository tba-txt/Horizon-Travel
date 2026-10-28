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
    @io.swagger.v3.oas.annotations.media.Schema(description = "Duração fixa do voo direto entre São Paulo (GRU) e o destino, em minutos.", example = "140")
    private Integer durationMinutes;
    private String flightNumber;
    @io.swagger.v3.oas.annotations.media.Schema(description = "Tarifa básica por passageiro para este único trecho (ida ou volta).")
    private BigDecimal pricePerPerson;
    @io.swagger.v3.oas.annotations.media.Schema(description = "Tarifa executiva por passageiro para este único trecho (ida ou volta).")
    private BigDecimal priceExecutive;
    @io.swagger.v3.oas.annotations.media.Schema(description = "Tarifa premium por passageiro para este único trecho (ida ou volta).")
    private BigDecimal pricePremium;
    private Integer totalSeats;
    private Integer availableSeats;
    private String seatClass;
}
