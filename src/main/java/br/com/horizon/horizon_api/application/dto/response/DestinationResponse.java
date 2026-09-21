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
    private java.math.BigDecimal basePrice;
    private String imageUrl;
}
