package br.com.horizon.horizon_api.application.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DestinationResponse {
    private Long id;
    private String name;
    private String country;
    private String city;
    private String tourismType;
    @io.swagger.v3.oas.annotations.media.Schema(description = "Preço-base estimado para ida e volta na tarifa básica (econômica). Não representa o preço de um único trecho.")
    private java.math.BigDecimal basePrice;
    private String imageUrl;
}
