package br.com.horizon.horizon_api.domain.port;

import br.com.horizon.horizon_api.domain.model.Reservation;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.ReservationStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservationPersistencePort {
    Reservation save(Reservation reservation);
    Optional<Reservation> findById(Long id);
    Optional<Reservation> findByIdAndUserId(Long id, Long userId);
    List<Reservation> findByUserId(Long userId);
    List<Reservation> findExpiredPendingReservations(OffsetDateTime now);
    List<Reservation> findConfirmedReservationsPastEndDate(java.time.LocalDate currentDate, java.time.LocalTime currentTime);
}
