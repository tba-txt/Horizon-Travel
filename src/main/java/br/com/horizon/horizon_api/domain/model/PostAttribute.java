package br.com.horizon.horizon_api.domain.model;
import lombok.Getter;
import lombok.Setter;
@Getter @Setter
public class PostAttribute {
    private Long postId;
    private Long attributeId;
    private String attributeName;
    private java.math.BigDecimal weight;
}
