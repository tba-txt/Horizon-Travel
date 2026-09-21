package br.com.horizon.horizon_api.application.dto.response;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
@Getter @Setter @AllArgsConstructor
public class ProfileAttributeDTO {
    private Long attributeId;
    private BigDecimal score;
}
