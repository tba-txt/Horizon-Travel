package br.com.horizon.horizon_api.domain.model;
import lombok.Getter;
import lombok.Setter;
@Getter @Setter
public class Interaction {
    private Long id; private Long userId; private Long postId; private br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.InteractionType interactionType; private java.time.OffsetDateTime createdAt;
}
