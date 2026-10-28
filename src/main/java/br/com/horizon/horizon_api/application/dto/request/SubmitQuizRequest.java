package br.com.horizon.horizon_api.application.dto.request;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.util.Map;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter @Setter
public class SubmitQuizRequest {
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Long userId;

    @Schema(description = "Respostas do quiz (Id da pergunta -> Id da resposta ou lista de Ids)", type = "object", example = "{\"1\": 1, \"2\": 3, \"3\": [7, 8]}")
    @NotNull private Map<Long, Object> answers;
    
    @Schema(description = "Orçamento disponível por pessoa", example = "5000.00")
    @NotNull(message = "O orçamento é obrigatório")
    @jakarta.validation.constraints.DecimalMin(value = "1000.00", message = "O orçamento mínimo é R$ 1000.00")
    @jakarta.validation.constraints.DecimalMax(value = "200000.00", message = "O orçamento máximo é R$ 200000.00")
    private java.math.BigDecimal budgetPerPerson;

    @Schema(description = "Indica se o viajante possui passaporte válido para voos internacionais", example = "true")
    private Boolean hasPassport = true;
}
