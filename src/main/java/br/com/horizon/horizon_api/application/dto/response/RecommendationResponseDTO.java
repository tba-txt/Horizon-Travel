package br.com.horizon.horizon_api.application.dto.response;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
@Getter @Setter
public class RecommendationResponseDTO {
    private Long destinationId;
    private String destinationName;
    private String country;
    private String city;
    private String tourismType;
    private String imageUrl;
    private BigDecimal basePrice;
    private BigDecimal score;
    private String explanation;
}
