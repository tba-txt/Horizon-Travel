package br.com.horizon.horizon_api.infrastructure.persistence.repository;

import br.com.horizon.horizon_api.infrastructure.persistence.entity.TicketEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<TicketEntity, Long> {
    @EntityGraph(attributePaths = {"passenger", "reservation"})
    List<TicketEntity> findByReservationId(Long reservationId);

    @EntityGraph(attributePaths = {"passenger", "reservation"})
    List<TicketEntity> findByReservation_IdIn(List<Long> reservationIds);

    @EntityGraph(attributePaths = {"passenger", "reservation"})
    Optional<TicketEntity> findByPassengerId(Long passengerId);

    boolean existsByPassengerId(Long passengerId);

    @EntityGraph(attributePaths = {"passenger", "reservation"})
    Optional<TicketEntity> findByTicketNumber(String ticketNumber);

    @Override
    @EntityGraph(attributePaths = {"passenger", "reservation"})
    Optional<TicketEntity> findById(Long id);
}
