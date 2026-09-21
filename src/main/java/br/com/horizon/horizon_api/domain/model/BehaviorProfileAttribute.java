package br.com.horizon.horizon_api.domain.model;
import lombok.Getter;
import lombok.Setter;
@Getter @Setter
public class BehaviorProfileAttribute {
    private Long id;
    private Long userId;
    private Long attributeId;
    private String attributeName;
    private java.math.BigDecimal score;
    private java.time.OffsetDateTime updatedAt;
}
