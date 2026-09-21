package br.com.horizon.horizon_api.interfaces.rest;

import br.com.horizon.horizon_api.application.dto.response.FlightResponse;
import br.com.horizon.horizon_api.application.usecase.SearchFlightsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Parameter;

import java.time.LocalDate;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/flights")
@RequiredArgsConstructor
public class FlightController {

    private final SearchFlightsUseCase searchFlightsUseCase;

    @GetMapping
    public ResponseEntity<List<FlightResponse>> searchFlights(
            @RequestParam Long destinationId,
            @Parameter(example = "2026-11-10") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(example = "2026-11-20") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("Recebida requisição GET /flights para destinationId={}, startDate={}, endDate={}", destinationId, startDate, endDate);
        List<FlightResponse> result = searchFlightsUseCase.execute(destinationId, startDate, endDate);
        log.info("Retornando {} voos", result.size());
        return ResponseEntity.ok(result);
    }
}
