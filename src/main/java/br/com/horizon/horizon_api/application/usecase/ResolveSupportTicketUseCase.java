package br.com.horizon.horizon_api.application.usecase;

import br.com.horizon.horizon_api.application.dto.response.SupportTicketResponse;
import br.com.horizon.horizon_api.domain.model.SupportTicket;
import br.com.horizon.horizon_api.domain.port.SupportTicketPersistencePort;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class ResolveSupportTicketUseCase {

    private final SupportTicketPersistencePort persistencePort;

    public ResolveSupportTicketUseCase(SupportTicketPersistencePort persistencePort) {
        this.persistencePort = persistencePort;
    }

    public SupportTicketResponse execute(Long id) {
        SupportTicket ticket = persistencePort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Chamado não encontrado: " + id));

        ticket.setStatus("RESOLVIDO");
        ticket.setUpdatedAt(OffsetDateTime.now());

        SupportTicket saved = persistencePort.save(ticket);
        return CreateSupportTicketUseCase.toResponse(saved);
    }
}
