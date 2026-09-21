package br.com.horizon.horizon_api.application.dto.request;

import lombok.Getter;
import lombok.Setter;

import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Setter
@Schema(description = "Requisição para feedback")
public class FeedbackRequest {
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Long userId;
    
    @Schema(description = "ID da reserva relacionada (opcional)", example = "1", nullable = true)
    private Long reservationId;
    
    @Schema(description = "Tipo de feedback (SUGGESTION, COMPLAINT, PRAISE)", example = "PRAISE", implementation = br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.FeedbackType.class)
    @jakarta.validation.constraints.NotNull
    private br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.FeedbackType feedbackType;
    
    @Schema(description = "Alvo do feedback (PLATFORM, DESTINATION, FLIGHT)", example = "PLATFORM", implementation = br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.TargetType.class)
    @jakarta.validation.constraints.NotNull
    private br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.TargetType targetType;
    
    @Schema(description = "Nota de 1 a 5", example = "5")
    @jakarta.validation.constraints.NotNull
    private Integer score;
    
    @Schema(description = "Comentário adicional", example = "Muito bom!")
    private String comment;
}
