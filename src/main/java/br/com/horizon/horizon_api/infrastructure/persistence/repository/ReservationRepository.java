package br.com.horizon.horizon_api.infrastructure.persistence.repository;

import br.com.horizon.horizon_api.infrastructure.persistence.entity.ReservationEntity;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<ReservationEntity, Long> {
    List<ReservationEntity> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<ReservationEntity> findByIdAndUserId(Long id, Long userId);
    List<ReservationEntity> findByStatusAndPaymentDeadlineBefore(ReservationStatus status, OffsetDateTime time);

    @org.springframework.data.jpa.repository.Query("SELECT r FROM ReservationEntity r WHERE r.status = 'CONFIRMADA' AND (" +
           "(r.tripType = 'ONE_WAY' AND (r.outboundFlight.flightDate < :currentDate OR (r.outboundFlight.flightDate = :currentDate AND (r.outboundFlight.arrivalTime IS NULL OR r.outboundFlight.arrivalTime <= :currentTime)))) OR " +
           "(r.tripType = 'ROUND_TRIP' AND (r.returnFlight.flightDate < :currentDate OR (r.returnFlight.flightDate = :currentDate AND (r.returnFlight.arrivalTime IS NULL OR r.returnFlight.arrivalTime <= :currentTime)))) )")
    List<ReservationEntity> findConfirmedReservationsPastEndDate(@org.springframework.data.repository.query.Param("currentDate") java.time.LocalDate currentDate, @org.springframework.data.repository.query.Param("currentTime") java.time.LocalTime currentTime);
}
