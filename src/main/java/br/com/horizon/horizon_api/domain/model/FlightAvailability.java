package br.com.horizon.horizon_api.domain.model;

import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;

@Getter
@Setter
public class FlightAvailability {
    private Long id;
    private Long flightId;
    private Integer totalSeats = 100;
    private Integer availableSeats = 100;
    private OffsetDateTime updatedAt;
}
