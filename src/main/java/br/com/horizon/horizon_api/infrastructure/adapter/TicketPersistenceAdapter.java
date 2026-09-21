package br.com.horizon.horizon_api.infrastructure.adapter;

import br.com.horizon.horizon_api.application.mapper.TicketMapper;
import br.com.horizon.horizon_api.domain.model.Ticket;
import br.com.horizon.horizon_api.domain.port.TicketPersistencePort;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.TicketEntity;
import br.com.horizon.horizon_api.infrastructure.persistence.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TicketPersistenceAdapter implements TicketPersistencePort {

    private final TicketRepository repository;
    private final TicketMapper mapper;

    @Override
    public Ticket save(Ticket domain) {
        TicketEntity entity = mapper.toEntity(domain);
        TicketEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<Ticket> saveAll(List<Ticket> tickets) {
        List<TicketEntity> entities = tickets.stream().map(mapper::toEntity).collect(Collectors.toList());
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
    public void delete(Long id) {
        repository.deleteById(id);
    }

    @Override
    public Optional<Ticket> findByTicketNumber(String ticketNumber) {
        return repository.findByTicketNumber(ticketNumber).map(mapper::toDomain);
    }
}
