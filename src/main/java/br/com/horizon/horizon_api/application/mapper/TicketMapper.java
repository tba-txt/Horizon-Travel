package br.com.horizon.horizon_api.application.mapper;

import br.com.horizon.horizon_api.application.dto.request.TicketRequest;
import br.com.horizon.horizon_api.application.dto.response.TicketResponse;
import br.com.horizon.horizon_api.domain.model.Ticket;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.PassengerEntity;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.ReservationEntity;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.TicketEntity;
import org.springframework.stereotype.Component;

@Component
public class TicketMapper {

    public Ticket toDomain(TicketEntity entity) {
        if (entity == null) return null;
        Ticket domain = new Ticket();
        domain.setId(entity.getId());
        if (entity.getReservation() != null) {
            domain.setReservationId(entity.getReservation().getId());
        }
        if (entity.getPassenger() != null) {
            domain.setPassengerId(entity.getPassenger().getId());
            domain.setPassengerName(entity.getPassenger().getName());
        }
        domain.setTicketNumber(entity.getTicketNumber());
        domain.setPdfUrl(entity.getPdfUrl());
        domain.setGeneratedAt(entity.getGeneratedAt());
        domain.setEmailSentAt(entity.getEmailSentAt());
        return domain;
    }

    public TicketEntity toEntity(Ticket domain) {
        if (domain == null) return null;
        TicketEntity entity = new TicketEntity();
        entity.setId(domain.getId());
        if (domain.getReservationId() != null) {
            ReservationEntity res = new ReservationEntity();
            res.setId(domain.getReservationId());
            entity.setReservation(res);
        }
        if (domain.getPassengerId() != null) {
            PassengerEntity pass = new PassengerEntity();
            pass.setId(domain.getPassengerId());
            entity.setPassenger(pass);
        }
        entity.setTicketNumber(domain.getTicketNumber());
        entity.setPdfUrl(domain.getPdfUrl());
        entity.setGeneratedAt(domain.getGeneratedAt());
        entity.setEmailSentAt(domain.getEmailSentAt());
        return entity;
    }

    public Ticket toDomain(TicketRequest request) {
        if (request == null) return null;
        Ticket ticket = new Ticket();
        ticket.setReservationId(request.getReservationId());
        ticket.setPassengerId(request.getPassengerId());
        return ticket;
    }

    public TicketResponse toResponse(Ticket domain) {
        if (domain == null) return null;
        TicketResponse response = new TicketResponse();
        response.setId(domain.getId());
        response.setReservationId(domain.getReservationId());
        response.setPassengerId(domain.getPassengerId());
        response.setPassengerName(domain.getPassengerName());
        response.setTicketNumber(domain.getTicketNumber());
        response.setPdfUrl(domain.getPdfUrl());
        response.setGeneratedAt(domain.getGeneratedAt());
        response.setEmailSentAt(domain.getEmailSentAt());
        return response;
    }
}
