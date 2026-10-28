package br.com.horizon.horizon_api.domain.model;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class Destination {
    private Long id;
    private String name;
    private String country;
    private String city;
    private String tourismType;
    private String imageUrl;
    private java.math.BigDecimal basePrice;
    private java.math.BigDecimal latitude;
    private java.math.BigDecimal longitude;
    private Boolean active;
    private Boolean requiresPassport;
    private java.time.OffsetDateTime createdAt;
    private List<DestinationAttribute> attributes;

    public boolean canVisitWithoutPassport() {
        return Boolean.FALSE.equals(this.requiresPassport);
    }
}
