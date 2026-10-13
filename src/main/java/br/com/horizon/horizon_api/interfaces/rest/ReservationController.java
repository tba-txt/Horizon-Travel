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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/reservations")
@RequiredArgsConstructor
@Tag(name = "Reservas", description = "Endpoints para criação, consulta e cancelamento de reservas")
public class ReservationController {

    private final CreateReservationUseCase createReservationUseCase;
    private final GetReservationByIdUseCase getReservationByIdUseCase;
    private final ListUserReservationsUseCase listUserReservationsUseCase;
    private final CancelReservationUseCase cancelReservationUseCase;
    private final GetReservationTicketsUseCase getReservationTicketsUseCase;

    @Operation(summary = "Criar reserva / Checkout", description = "Cria uma nova reserva para o usuário autenticado com voos e passageiros.")
    @PostMapping("/checkout")
    public ResponseEntity<ReservationResponse> checkout(
            @Valid @RequestBody ReservationRequest request) {
        Long userId = SecurityUtils.getAuthenticatedUserId();
        log.info("Recebida requisição POST /reservations/checkout para userId={}, request={}", userId, request);
        ReservationResponse response = createReservationUseCase.execute(userId, request);
        log.info("Reserva criada com sucesso, reservationId={}", response.getReservationId());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Obter reserva por ID", description = "Retorna os detalhes de uma reserva específica pertencente ao usuário autenticado.")
    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponse> getById(@PathVariable Long id) {
        Long userId = SecurityUtils.getAuthenticatedUserId();
        return ResponseEntity.ok(getReservationByIdUseCase.execute(id, userId));
    }

    @Operation(summary = "Minhas viagens / Listar reservas", description = "Retorna todas as reservas do usuário autenticado agrupadas por status.")
    @GetMapping("/my")
    public ResponseEntity<MyTripsResponse> listMyReservations() {
        Long userId = SecurityUtils.getAuthenticatedUserId();
        return ResponseEntity.ok(listUserReservationsUseCase.executeMyTrips(userId));
    }

    @Operation(summary = "Listar reservas pendentes", description = "Retorna exclusivamente as reservas com status PENDENTE do usuário autenticado.")
    @GetMapping("/pending")
    public ResponseEntity<List<ReservationResponse>> listPendingReservations() {
        Long userId = SecurityUtils.getAuthenticatedUserId();
        MyTripsResponse myTrips = listUserReservationsUseCase.executeMyTrips(userId);
        return ResponseEntity.ok(myTrips.getPending());
    }

    @Operation(summary = "Listar bilhetes da reserva", description = "Retorna os bilhetes de todos os passageiros de uma reserva específica.")
    @GetMapping("/{id}/tickets")
    public ResponseEntity<List<TicketResponse>> getReservationTickets(@PathVariable Long id) {
        Long userId = SecurityUtils.getAuthenticatedUserId();
        return ResponseEntity.ok(getReservationTicketsUseCase.execute(id, userId));
    }

    @Operation(summary = "Cancelar reserva", description = "Cancela uma reserva e libera os assentos dos voos vinculados.")
    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        Long userId = SecurityUtils.getAuthenticatedUserId();
        cancelReservationUseCase.execute(id, userId);
        return ResponseEntity.ok().build();
    }
}
