package br.com.horizon.horizon_api.application.mapper;

import br.com.horizon.horizon_api.domain.model.Interaction;
import br.com.horizon.horizon_api.application.dto.request.InteractionRequest;
import br.com.horizon.horizon_api.application.dto.response.InteractionResponse;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.InteractionEntity;
import org.springframework.stereotype.Component;

@Component
public class InteractionMapper {
    
    public Interaction toDomain(InteractionEntity entity) {
        if (entity == null) return null;
        Interaction domain = new Interaction();
        domain.setId(entity.getId());
        return domain;
    }
    
    public InteractionEntity toEntity(Interaction domain) {
        if (domain == null) return null;
        InteractionEntity entity = new InteractionEntity();
        entity.setId(domain.getId());
        return entity;
    }
    
    public Interaction toDomain(InteractionRequest request) {
        if (request == null) return null;
        return new Interaction();
    }
    
    public InteractionResponse toResponse(Interaction domain) {
        if (domain == null) return null;
        InteractionResponse response = new InteractionResponse();
        response.setId(domain.getId());
        return response;
    }
}
