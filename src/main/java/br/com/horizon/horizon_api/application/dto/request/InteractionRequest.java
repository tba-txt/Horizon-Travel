package br.com.horizon.horizon_api.application.dto.request;

import br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.InteractionType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter @Setter
@Schema(description = "Requisição para interações")
public class InteractionRequest {
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Long userId;

    @Schema(description = "ID do post", example = "1")
    @NotNull private Long postId;

    @Schema(description = "Tipo de interação (aceita apenas LIKE ou DISLIKE)", example = "LIKE", allowableValues = {"LIKE", "DISLIKE"})
    @NotNull(message = "interactionType é obrigatório (LIKE ou DISLIKE)")
    private InteractionType interactionType;
}
