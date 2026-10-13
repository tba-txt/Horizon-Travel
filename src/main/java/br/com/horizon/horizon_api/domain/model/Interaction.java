package br.com.horizon.horizon_api.domain.model;

import br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.InteractionType;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;

@Getter
@Setter
public class Interaction {
    private Long id;
    private Long userId;
    private Long postId;
    private Long optionId;
    private InteractionType interactionType;
    private OffsetDateTime createdAt;
}