package br.com.horizon.horizon_api.domain.port;

import br.com.horizon.horizon_api.domain.model.Ticket;

import java.util.List;
import java.util.Optional;

public interface TicketPersistencePort {
    Ticket save(Ticket ticket);
    List<Ticket> saveAll(List<Ticket> tickets);
    Optional<Ticket> findById(Long id);
    Optional<Ticket> findByPassengerId(Long passengerId);
    List<Ticket> findByReservationId(Long reservationId);
    boolean existsByPassengerId(Long passengerId);
    void delete(Long id);
    java.util.Optional<Ticket> findByTicketNumber(String ticketNumber);
}
