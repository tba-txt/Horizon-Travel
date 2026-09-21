package br.com.horizon.horizon_api.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "answer_attributes")
@Getter
@Setter
public class AnswerAttributeEntity {

    @EmbeddedId
    private AnswerAttributeId id = new AnswerAttributeId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("answerId")
    @JoinColumn(name = "answer_id")
    private AnswerEntity answer;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("attributeId")
    @JoinColumn(name = "attribute_id")
    private AttributeEntity attribute;

    @Column(name = "weight", precision = 4, scale = 3)
    private BigDecimal weight;
}
