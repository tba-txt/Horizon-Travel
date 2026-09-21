package br.com.horizon.horizon_api.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "user_profile_attributes", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "attribute_id"})
})
@Getter
@Setter
public class UserProfileAttributeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attribute_id", nullable = false)
    private AttributeEntity attribute;

    @Column(name = "score", precision = 4, scale = 3)
    private BigDecimal score;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
