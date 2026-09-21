package br.com.horizon.horizon_api.infrastructure.persistence.repository;

import br.com.horizon.horizon_api.infrastructure.persistence.entity.TicketEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<TicketEntity, Long> {
    List<TicketEntity> findByReservationId(Long reservationId);
    Optional<TicketEntity> findByPassengerId(Long passengerId);
    boolean existsByPassengerId(Long passengerId);
    Optional<TicketEntity> findByTicketNumber(String ticketNumber);
}
