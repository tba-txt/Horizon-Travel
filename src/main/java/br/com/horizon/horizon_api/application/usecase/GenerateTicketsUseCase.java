package br.com.horizon.horizon_api.application.usecase;

import br.com.horizon.horizon_api.domain.model.Reservation;
import br.com.horizon.horizon_api.domain.model.Ticket;

import java.util.List;

public interface GenerateTicketsUseCase {
    List<Ticket> execute(Reservation reservation);
}
