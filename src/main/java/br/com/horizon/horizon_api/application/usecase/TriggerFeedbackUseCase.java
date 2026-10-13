package br.com.horizon.horizon_api.application.usecase;

import br.com.horizon.horizon_api.domain.model.Reservation;

public interface TriggerFeedbackUseCase {
    void execute(Reservation reservation);
}
