package br.com.horizon.horizon_api.application.usecase;

import br.com.horizon.horizon_api.application.dto.response.SupportTicketResponse;
import br.com.horizon.horizon_api.domain.model.SupportTicket;
import br.com.horizon.horizon_api.domain.port.SupportTicketPersistencePort;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class GetSupportTicketsUseCase {

    private final SupportTicketPersistencePort persistencePort;

    public GetSupportTicketsUseCase(SupportTicketPersistencePort persistencePort) {
        this.persistencePort = persistencePort;
    }

    public List<SupportTicketResponse> execute(Long userId, String email) {
        List<SupportTicket> list = Collections.emptyList();
        if (userId != null) {
            list = persistencePort.findByUserId(userId);
        } else if (email != null && !email.isBlank()) {
            list = persistencePort.findByUserEmail(email.trim().toLowerCase());
        }

        return list.stream()
                .map(CreateSupportTicketUseCase::toResponse)
                .toList();
    }
}
