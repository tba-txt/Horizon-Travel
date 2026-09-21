package br.com.horizon.horizon_api.application.usecase;

import br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.PaymentMethod;

public interface ConfirmPaymentUseCase {
    void execute(Long reservationId, Long userId, PaymentMethod method);
}
