package br.com.horizon.horizon_api.domain.model;
import lombok.Getter;
import lombok.Setter;
@Getter @Setter
public class DestinationAttribute {
    private Long attributeId;
    private String attributeName;
    private java.math.BigDecimal score;
}
