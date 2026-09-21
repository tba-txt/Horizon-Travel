package br.com.horizon.horizon_api.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.TripType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ReservationRequest {
    @Schema(description = "ID do destino", example = "1")
    @NotNull(message = "Destination ID is required")
    private Long destinationId;
    
    @Schema(description = "Tipo da viagem", example = "ROUND_TRIP")
    @NotNull(message = "Trip type is required")
    private TripType tripType;
    
    @Schema(description = "ID do voo de ida", example = "1")
    @NotNull(message = "Outbound flight ID is required")
    private Long outboundFlightId;
    
    @Schema(description = "ID do voo de volta (opcional para ONE_WAY)", example = "2", nullable = true)
    private Long returnFlightId;
    
    @Schema(description = "Lista de passageiros. O comprador é o usuário autenticado (via JWT).")
    @NotEmpty(message = "At least one passenger is required")
    @Size(min = 1, max = 5, message = "Reservation must have between 1 and 5 passengers")
    private List<PassengerRequest> passengers;
}
