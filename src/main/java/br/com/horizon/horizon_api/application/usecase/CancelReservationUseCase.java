package br.com.horizon.horizon_api.application.usecase;

public interface CancelReservationUseCase {
    void execute(Long id, Long userId);
}
