package br.com.horizon.horizon_api.application.service;

import br.com.horizon.horizon_api.domain.model.Passenger;
import br.com.horizon.horizon_api.domain.port.PassengerPersistencePort;
import br.com.horizon.horizon_api.application.usecase.CreatePassengerUseCase;
import br.com.horizon.horizon_api.application.usecase.GetReservationPassengersUseCase;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PassengerService implements CreatePassengerUseCase, GetReservationPassengersUseCase {
    
    private final PassengerPersistencePort port;

    // TODO: Implement Use Case methods
}
