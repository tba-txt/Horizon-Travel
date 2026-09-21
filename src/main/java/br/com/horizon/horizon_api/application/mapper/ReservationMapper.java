package br.com.horizon.horizon_api.application.mapper;

import br.com.horizon.horizon_api.domain.model.Reservation;
import br.com.horizon.horizon_api.application.dto.request.ReservationRequest;
import br.com.horizon.horizon_api.application.dto.response.ReservationResponse;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.ReservationEntity;
import org.springframework.stereotype.Component;

@Component
public class ReservationMapper {
    
    public Reservation toDomain(ReservationEntity entity) {
        if (entity == null) return null;
        Reservation domain = new Reservation();
        domain.setId(entity.getId());
        return domain;
    }
    
    public ReservationEntity toEntity(Reservation domain) {
        if (domain == null) return null;
        ReservationEntity entity = new ReservationEntity();
        entity.setId(domain.getId());
        return entity;
    }
    
    public Reservation toDomain(ReservationRequest request) {
        if (request == null) return null;
        return new Reservation();
    }
    
    public ReservationResponse toResponse(Reservation domain) {
        if (domain == null) return null;
        ReservationResponse response = new ReservationResponse();
        response.setReservationId(domain.getId());
        return response;
    }
}
