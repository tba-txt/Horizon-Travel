package br.com.horizon.horizon_api.application.dto.response;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
@Getter @Setter
public class PostAttributeDTO {
    private Long attributeId;
    private String attributeName;
    private BigDecimal weight;
}
