package br.com.horizon.horizon_api.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "flight_availability")
@Getter
@Setter
public class FlightAvailabilityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_id", unique = true, nullable = false)
    private FlightEntity flight;

    @Column(name = "total_seats")
    private Integer totalSeats = 100;

    @Column(name = "available_seats")
    private Integer availableSeats = 100;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (totalSeats == null) {
            totalSeats = 100;
        }
        if (availableSeats == null) {
            availableSeats = totalSeats;
        }
        if (updatedAt == null) {
            updatedAt = OffsetDateTime.now();
        }
    }
}
