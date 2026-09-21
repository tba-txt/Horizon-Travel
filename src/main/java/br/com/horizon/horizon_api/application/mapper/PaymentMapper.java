package br.com.horizon.horizon_api.application.mapper;

import br.com.horizon.horizon_api.domain.model.Payment;
import br.com.horizon.horizon_api.application.dto.request.PaymentRequest;
import br.com.horizon.horizon_api.application.dto.response.PaymentResponse;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.PaymentEntity;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {
    
    public Payment toDomain(PaymentEntity entity) {
        if (entity == null) return null;
        Payment domain = new Payment();
        domain.setId(entity.getId());
        return domain;
    }
    
    public PaymentEntity toEntity(Payment domain) {
        if (domain == null) return null;
        PaymentEntity entity = new PaymentEntity();
        entity.setId(domain.getId());
        return entity;
    }
    
    public Payment toDomain(PaymentRequest request) {
        if (request == null) return null;
        return new Payment();
    }
    
    public PaymentResponse toResponse(Payment domain) {
        if (domain == null) return null;
        PaymentResponse response = new PaymentResponse();
        response.setId(domain.getId());
        return response;
    }
}
