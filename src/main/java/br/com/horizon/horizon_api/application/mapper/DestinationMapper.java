package br.com.horizon.horizon_api.application.mapper;

import br.com.horizon.horizon_api.domain.model.Destination;
import br.com.horizon.horizon_api.application.dto.request.DestinationRequest;
import br.com.horizon.horizon_api.application.dto.response.DestinationResponse;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.DestinationEntity;
import org.springframework.stereotype.Component;

@Component
public class DestinationMapper {
    
    public Destination toDomain(DestinationEntity entity) {
        if (entity == null) return null;
        Destination domain = new Destination();
        domain.setId(entity.getId());
        return domain;
    }
    
    public DestinationEntity toEntity(Destination domain) {
        if (domain == null) return null;
        DestinationEntity entity = new DestinationEntity();
        entity.setId(domain.getId());
        return entity;
    }
    
    public Destination toDomain(DestinationRequest request) {
        if (request == null) return null;
        return new Destination();
    }
    
    public DestinationResponse toResponse(Destination domain) {
        if (domain == null) return null;
        DestinationResponse response = new DestinationResponse();
        response.setId(domain.getId());
        return response;
    }
}
