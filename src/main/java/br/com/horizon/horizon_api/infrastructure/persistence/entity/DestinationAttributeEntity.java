package br.com.horizon.horizon_api.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "destination_attributes")
@Getter
@Setter
public class DestinationAttributeEntity {

    @EmbeddedId
    private DestinationAttributeId id = new DestinationAttributeId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("destinationId")
    @JoinColumn(name = "destination_id")
    private DestinationEntity destination;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("attributeId")
    @JoinColumn(name = "attribute_id")
    private AttributeEntity attribute;

    @Column(name = "score", precision = 4, scale = 3)
    private BigDecimal score;
}
