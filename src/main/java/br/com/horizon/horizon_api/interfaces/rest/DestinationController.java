package br.com.horizon.horizon_api.interfaces.rest;

import br.com.horizon.horizon_api.application.dto.response.DestinationResponse;
import br.com.horizon.horizon_api.application.dto.response.DestinationWeatherResponse;
import br.com.horizon.horizon_api.application.usecase.GetDestinationByIdUseCase;
import br.com.horizon.horizon_api.application.usecase.GetDestinationWeatherUseCase;
import br.com.horizon.horizon_api.application.usecase.ListDestinationsUseCase;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/destinations")
@RequiredArgsConstructor
public class DestinationController {

    private final GetDestinationWeatherUseCase getDestinationWeatherUseCase;
    private final GetDestinationByIdUseCase getDestinationByIdUseCase;
    private final ListDestinationsUseCase listDestinationsUseCase;

    @GetMapping("/{id}")
    public ResponseEntity<DestinationResponse> getById(@PathVariable Long id) {
        log.info("Recebida requisição GET /destinations/{}", id);
        return ResponseEntity.ok(getDestinationByIdUseCase.execute(id));
    }

    @GetMapping
    public ResponseEntity<List<DestinationResponse>> list() {
        log.info("Recebida requisição GET /destinations");
        List<DestinationResponse> result = listDestinationsUseCase.execute();
        log.info("Retornando {} destinos", result.size());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}/weather")
    public ResponseEntity<DestinationWeatherResponse> getWeather(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate travelDate) {
        return ResponseEntity.ok(getDestinationWeatherUseCase.execute(id, travelDate));
    }
}

