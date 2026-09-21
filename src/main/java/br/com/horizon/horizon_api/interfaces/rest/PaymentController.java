package br.com.horizon.horizon_api.interfaces.rest;

import br.com.horizon.horizon_api.application.usecase.ConfirmPaymentUseCase;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.PaymentMethod;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final ConfirmPaymentUseCase confirmPaymentUseCase;

    @PostMapping("/{reservationId}/confirm")
    public ResponseEntity<Void> confirmPayment(
            @PathVariable Long reservationId,
            @RequestParam PaymentMethod method) {
        Long userId = SecurityUtils.getAuthenticatedUserId();
        log.info("Recebida requisição POST /payments/{}/confirm para userId={}, method={}", reservationId, userId, method);
        confirmPaymentUseCase.execute(reservationId, userId, method);
        log.info("Pagamento confirmado para reservationId={}", reservationId);
        return ResponseEntity.ok().build();
    }
}
