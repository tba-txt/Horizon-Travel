package br.com.horizon.horizon_api.application.service;

import br.com.horizon.horizon_api.domain.model.Destination;
import br.com.horizon.horizon_api.domain.port.DestinationPersistencePort;
import br.com.horizon.horizon_api.application.usecase.CreateDestinationUseCase;
import br.com.horizon.horizon_api.application.usecase.GetDestinationByIdUseCase;
import br.com.horizon.horizon_api.application.usecase.ListDestinationsUseCase;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import br.com.horizon.horizon_api.application.dto.response.DestinationResponse;
import br.com.horizon.horizon_api.domain.exception.ResourceNotFoundException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DestinationService implements CreateDestinationUseCase, GetDestinationByIdUseCase, ListDestinationsUseCase {
    
    private final DestinationPersistencePort port;

    @Override
    public List<DestinationResponse> execute() {
        return port.findActive().stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public DestinationResponse execute(Long id) {
        Destination dest = port.findById(id).orElseThrow(() -> new ResourceNotFoundException("Destino não encontrado."));
        return mapToResponse(dest);
    }

    private DestinationResponse mapToResponse(Destination dest) {
        DestinationResponse res = new DestinationResponse();
        res.setId(dest.getId());
        res.setName(dest.getName());
        res.setCountry(dest.getCountry());
        res.setCity(dest.getCity());
        res.setTourismType(dest.getTourismType());
        res.setBasePrice(dest.getBasePrice());
        res.setImageUrl(dest.getImageUrl());
        return res;
    }
    
    // TODO: Implement CreateDestinationUseCase if needed
}
