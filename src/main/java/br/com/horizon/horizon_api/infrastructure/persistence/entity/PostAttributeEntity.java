package br.com.horizon.horizon_api.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "post_attributes")
@Getter
@Setter
public class PostAttributeEntity {

    @EmbeddedId
    private PostAttributeId id = new PostAttributeId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("postId")
    @JoinColumn(name = "post_id")
    private PostEntity post;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("attributeId")
    @JoinColumn(name = "attribute_id")
    private AttributeEntity attribute;

    @Column(name = "weight", precision = 4, scale = 3)
    private BigDecimal weight;
}
