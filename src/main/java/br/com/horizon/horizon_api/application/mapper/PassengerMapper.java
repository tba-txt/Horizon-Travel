package br.com.horizon.horizon_api.application.mapper;

import br.com.horizon.horizon_api.domain.model.Passenger;
import br.com.horizon.horizon_api.application.dto.request.PassengerRequest;
import br.com.horizon.horizon_api.application.dto.response.PassengerResponse;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.PassengerEntity;
import org.springframework.stereotype.Component;

@Component
public class PassengerMapper {
    
    public Passenger toDomain(PassengerEntity entity) {
        if (entity == null) return null;
        Passenger domain = new Passenger();
        domain.setId(entity.getId());
        return domain;
    }
    
    public PassengerEntity toEntity(Passenger domain) {
        if (domain == null) return null;
        PassengerEntity entity = new PassengerEntity();
        entity.setId(domain.getId());
        return entity;
    }
    
    public Passenger toDomain(PassengerRequest request) {
        if (request == null) return null;
        return new Passenger();
    }
    
    public PassengerResponse toResponse(Passenger domain) {
        if (domain == null) return null;
        PassengerResponse response = new PassengerResponse();
        response.setId(domain.getId());
        return response;
    }
}
