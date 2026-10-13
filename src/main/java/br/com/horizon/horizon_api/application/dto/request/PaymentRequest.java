package br.com.horizon.horizon_api.application.dto.request;

import br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {
    @Schema(description = "Método de pagamento desejado (PIX, CARD)", example = "PIX")
    @NotNull(message = "O método de pagamento é obrigatório.")
    private PaymentMethod paymentMethod;
}
