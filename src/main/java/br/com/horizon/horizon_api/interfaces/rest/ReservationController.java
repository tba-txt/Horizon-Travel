package br.com.horizon.horizon_api.interfaces.rest;

import br.com.horizon.horizon_api.application.dto.request.ReservationRequest;
import br.com.horizon.horizon_api.application.dto.response.MyTripsResponse;
import br.com.horizon.horizon_api.application.dto.response.ReservationResponse;
import br.com.horizon.horizon_api.application.dto.response.TicketResponse;
import br.com.horizon.horizon_api.application.usecase.CancelReservationUseCase;
import br.com.horizon.horizon_api.application.usecase.CreateReservationUseCase;
import br.com.horizon.horizon_api.application.usecase.GetReservationByIdUseCase;
import br.com.horizon.horizon_api.application.usecase.GetReservationTicketsUseCase;
import br.com.horizon.horizon_api.application.usecase.ListUserReservationsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final CreateReservationUseCase createReservationUseCase;
    private final GetReservationByIdUseCase getReservationByIdUseCase;
    private final ListUserReservationsUseCase listUserReservationsUseCase;
    private final CancelReservationUseCase cancelReservationUseCase;
    private final GetReservationTicketsUseCase getReservationTicketsUseCase;

    @PostMapping("/checkout")
    public ResponseEntity<ReservationResponse> checkout(
            @Valid @RequestBody ReservationRequest request) {
        Long userId = SecurityUtils.getAuthenticatedUserId();
        log.info("Recebida requisição POST /reservations/checkout para userId={}, request={}", userId, request);
        ReservationResponse response = createReservationUseCase.execute(userId, request);
        log.info("Reserva criada com sucesso, reservationId={}", response.getReservationId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponse> getById(@PathVariable Long id) {
        Long userId = SecurityUtils.getAuthenticatedUserId();
        return ResponseEntity.ok(getReservationByIdUseCase.execute(id, userId));
    }

    @GetMapping("/my")
    public ResponseEntity<MyTripsResponse> listMyReservations() {
        Long userId = SecurityUtils.getAuthenticatedUserId();
        return ResponseEntity.ok(listUserReservationsUseCase.executeMyTrips(userId));
    }

    @GetMapping("/{id}/tickets")
    public ResponseEntity<List<TicketResponse>> getReservationTickets(@PathVariable Long id) {
        Long userId = SecurityUtils.getAuthenticatedUserId();
        return ResponseEntity.ok(getReservationTicketsUseCase.execute(id, userId));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        Long userId = SecurityUtils.getAuthenticatedUserId();
        cancelReservationUseCase.execute(id, userId);
        return ResponseEntity.ok().build();
    }
}
