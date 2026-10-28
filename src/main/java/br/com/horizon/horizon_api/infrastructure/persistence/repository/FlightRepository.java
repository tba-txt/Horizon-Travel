package br.com.horizon.horizon_api.infrastructure.persistence.repository;

import br.com.horizon.horizon_api.infrastructure.persistence.entity.FlightEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FlightRepository extends JpaRepository<FlightEntity, Long> {
    
    // We join FlightAvailability to prevent N+1 and we filter availableSeats > 0
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"destination"})
    @Query("SELECT f FROM FlightEntity f JOIN FETCH f.availability fa " +
           "WHERE f.destination.id = :destinationId AND f.active = true " +
           "AND f.flightDate BETWEEN :start AND :end " +
           "AND fa.availableSeats > 0 " +
           "ORDER BY f.flightDate ASC, f.departureTime ASC")
    List<FlightEntity> findAvailableFlights(
            @Param("destinationId") Long destinationId, 
            @Param("start") LocalDate start, 
            @Param("end") LocalDate end, org.springframework.data.domain.Pageable pageable);

    @Override
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"availability", "destination"})
    List<FlightEntity> findAll();

    Optional<FlightEntity> findByIdAndActiveTrue(Long id);

    Optional<FlightEntity> findByFlightNumber(String flightNumber);
}
