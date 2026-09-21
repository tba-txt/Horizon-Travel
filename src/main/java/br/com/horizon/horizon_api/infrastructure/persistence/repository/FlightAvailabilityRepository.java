package br.com.horizon.horizon_api.infrastructure.persistence.repository;

import br.com.horizon.horizon_api.infrastructure.persistence.entity.FlightAvailabilityEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FlightAvailabilityRepository extends JpaRepository<FlightAvailabilityEntity, Long> {
    Optional<FlightAvailabilityEntity> findByFlightId(Long flightId);

    @Modifying
    @Query("UPDATE FlightAvailabilityEntity f SET f.availableSeats = f.availableSeats - :seats, f.updatedAt = CURRENT_TIMESTAMP WHERE f.flight.id = :flightId AND f.availableSeats >= :seats")
    int reserveSeats(@Param("flightId") Long flightId, @Param("seats") int seats);

    @Modifying
    @Query("UPDATE FlightAvailabilityEntity f SET f.availableSeats = f.availableSeats + :seats, f.updatedAt = CURRENT_TIMESTAMP WHERE f.flight.id = :flightId AND f.availableSeats + :seats <= f.totalSeats")
    int releaseSeats(@Param("flightId") Long flightId, @Param("seats") int seats);
}
