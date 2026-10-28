package br.com.horizon.horizon_api.interfaces.rest;

import br.com.horizon.horizon_api.application.dto.request.SupportTicketRequest;
import br.com.horizon.horizon_api.application.dto.response.SupportTicketResponse;
import br.com.horizon.horizon_api.application.usecase.CreateSupportTicketUseCase;
import br.com.horizon.horizon_api.application.usecase.GetSupportTicketsUseCase;
import br.com.horizon.horizon_api.application.usecase.ResolveSupportTicketUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/support-tickets")
@RequiredArgsConstructor
@Tag(name = "Suporte e Chamados", description = "Endpoints para abertura, consulta e resolução de chamados de atendimento")
public class SupportTicketController {

    private final CreateSupportTicketUseCase createSupportTicketUseCase;
    private final GetSupportTicketsUseCase getSupportTicketsUseCase;
    private final ResolveSupportTicketUseCase resolveSupportTicketUseCase;

    @Operation(summary = "Abrir chamado de suporte", description = "Registra um novo chamado de atendimento, gera protocolo HZ-2026-XX e envia e-mail com SLA.")
    @PostMapping
    public ResponseEntity<SupportTicketResponse> createTicket(@Valid @RequestBody SupportTicketRequest request) {
        Long authenticatedUserId = SecurityUtils.getOptionalAuthenticatedUserId();
        log.info("Recebida abertura de chamado: email={}, assunto={}, userId={}", request.getUserEmail(), request.getSubject(), authenticatedUserId);
        SupportTicketResponse response = createSupportTicketUseCase.execute(request, authenticatedUserId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Listar chamados do usuário autenticado", description = "Retorna todos os chamados abertos pelo usuário autenticado ou pelo e-mail informado.")
    @GetMapping
    public ResponseEntity<List<SupportTicketResponse>> getMyTickets(@RequestParam(required = false) String email) {
        Long authenticatedUserId = SecurityUtils.getOptionalAuthenticatedUserId();
        List<SupportTicketResponse> response = getSupportTicketsUseCase.execute(authenticatedUserId, email);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Marcar chamado como resolvido", description = "Atualiza o status de um chamado para RESOLVIDO.")
    @PatchMapping("/{id}/resolve")
    public ResponseEntity<SupportTicketResponse> resolveTicket(@PathVariable Long id) {
        log.info("Marcando chamado {} como resolvido", id);
        SupportTicketResponse response = resolveSupportTicketUseCase.execute(id);
        return ResponseEntity.ok(response);
    }
}
