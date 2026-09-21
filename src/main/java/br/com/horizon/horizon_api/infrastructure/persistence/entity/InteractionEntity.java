package br.com.horizon.horizon_api.infrastructure.persistence.entity;

import br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.InteractionType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "interactions")
@Getter
@Setter
public class InteractionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private PostEntity post;

    @Enumerated(EnumType.STRING)
    @Column(name = "interaction_type", length = 20)
    private InteractionType interactionType;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}
