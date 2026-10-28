package br.com.horizon.horizon_api.infrastructure.adapter;

import br.com.horizon.horizon_api.application.mapper.TicketMapper;
import br.com.horizon.horizon_api.domain.model.Ticket;
import br.com.horizon.horizon_api.domain.port.TicketPersistencePort;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.TicketEntity;
import br.com.horizon.horizon_api.infrastructure.persistence.repository.PassengerRepository;
import br.com.horizon.horizon_api.infrastructure.persistence.repository.ReservationRepository;
import br.com.horizon.horizon_api.infrastructure.persistence.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TicketPersistenceAdapter implements TicketPersistencePort {

    private final TicketRepository repository;
    private final ReservationRepository reservationRepository;
    private final PassengerRepository passengerRepository;
    private final TicketMapper mapper;

    @Override
    @Transactional
    public Ticket save(Ticket domain) {
        TicketEntity entity = mapper.toEntity(domain);
        if (domain.getReservationId() != null) {
            entity.setReservation(reservationRepository.findById(domain.getReservationId()).orElse(null));
        }
        if (domain.getPassengerId() != null) {
            entity.setPassenger(passengerRepository.findById(domain.getPassengerId()).orElse(null));
        }
        TicketEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional
    public List<Ticket> saveAll(List<Ticket> tickets) {
        List<TicketEntity> entities = tickets.stream().map(domain -> {
            TicketEntity entity = mapper.toEntity(domain);
            if (domain.getReservationId() != null) {
                entity.setReservation(reservationRepository.findById(domain.getReservationId()).orElse(null));
            }
            if (domain.getPassengerId() != null) {
                entity.setPassenger(passengerRepository.findById(domain.getPassengerId()).orElse(null));
            }
            return entity;
        }).collect(Collectors.toList());
        return repository.saveAll(entities).stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public Optional<Ticket> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Ticket> findByPassengerId(Long passengerId) {
        return repository.findByPassengerId(passengerId).map(mapper::toDomain);
    }

    @Override
    public List<Ticket> findByReservationIdIn(List<Long> reservationIds) {
        if (reservationIds == null || reservationIds.isEmpty()) return List.of();
        return repository.findByReservation_IdIn(reservationIds).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Ticket> findByReservationId(Long reservationId) {
        return repository.findByReservationId(reservationId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByPassengerId(Long passengerId) {
        return repository.existsByPassengerId(passengerId);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    @Override
    public Optional<Ticket> findByTicketNumber(String ticketNumber) {
        return repository.findByTicketNumber(ticketNumber).map(mapper::toDomain);
    }
}
