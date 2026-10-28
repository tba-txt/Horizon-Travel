package br.com.horizon.horizon_api.domain.port;

import br.com.horizon.horizon_api.domain.model.SupportTicket;

import java.util.List;
import java.util.Optional;

public interface SupportTicketPersistencePort {

    SupportTicket save(SupportTicket ticket);

    List<SupportTicket> findByUserId(Long userId);

    List<SupportTicket> findByUserEmail(String userEmail);

    Optional<SupportTicket> findById(Long id);

    Optional<SupportTicket> findByTicketCode(String ticketCode);

    long count();
}
