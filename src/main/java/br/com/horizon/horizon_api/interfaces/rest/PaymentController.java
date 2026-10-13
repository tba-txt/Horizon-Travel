package br.com.horizon.horizon_api.interfaces.rest;

import br.com.horizon.horizon_api.application.dto.request.PaymentRequest;
import br.com.horizon.horizon_api.application.usecase.ConfirmPaymentUseCase;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.PaymentMethod;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "Pagamentos", description = "Endpoints para confirmação de pagamento de reservas")
public class PaymentController {

    private final ConfirmPaymentUseCase confirmPaymentUseCase;

    @Operation(summary = "Confirmar pagamento da reserva", description = "Confirma o pagamento de uma reserva pendente, atualiza o status para CONFIRMADA e gera os bilhetes.")
    @PostMapping("/{reservationId}/confirm")
    public ResponseEntity<Void> confirmPayment(
            @PathVariable Long reservationId,
            @RequestParam(required = false) PaymentMethod method,
            @RequestBody(required = false) PaymentRequest request) {
        Long userId = SecurityUtils.getAuthenticatedUserId();
        PaymentMethod effectiveMethod = method;
        if (effectiveMethod == null && request != null) {
            effectiveMethod = request.getPaymentMethod();
        }
        if (effectiveMethod == null) {
            effectiveMethod = PaymentMethod.PIX;
        }

        log.info("Recebida requisição POST /payments/{}/confirm para userId={}, method={}", reservationId, userId, effectiveMethod);
        confirmPaymentUseCase.execute(reservationId, userId, effectiveMethod);
        log.info("Pagamento confirmado para reservationId={}", reservationId);
        return ResponseEntity.ok().build();
    }
}
