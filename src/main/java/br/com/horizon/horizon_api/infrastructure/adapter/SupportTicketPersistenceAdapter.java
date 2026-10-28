package br.com.horizon.horizon_api.infrastructure.adapter;

import br.com.horizon.horizon_api.domain.model.SupportTicket;
import br.com.horizon.horizon_api.domain.port.SupportTicketPersistencePort;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.SupportTicketEntity;
import br.com.horizon.horizon_api.infrastructure.persistence.repository.SupportTicketRepository;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class SupportTicketPersistenceAdapter implements SupportTicketPersistencePort {

    private final SupportTicketRepository repository;

    public SupportTicketPersistenceAdapter(SupportTicketRepository repository) {
        this.repository = repository;
    }

    @Override
    public SupportTicket save(SupportTicket ticket) {
        SupportTicketEntity entity = toEntity(ticket);
        SupportTicketEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<SupportTicket> findByUserId(Long userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<SupportTicket> findByUserEmail(String userEmail) {
        return repository.findByUserEmailOrderByCreatedAtDesc(userEmail).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<SupportTicket> findById(Long id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<SupportTicket> findByTicketCode(String ticketCode) {
        return repository.findByTicketCode(ticketCode).map(this::toDomain);
    }

    @Override
    public long count() {
        return repository.count();
    }

    private SupportTicket toDomain(SupportTicketEntity entity) {
        if (entity == null) return null;
        return new SupportTicket(
                entity.getId(),
                entity.getTicketCode(),
                entity.getUserId(),
                entity.getUserName(),
                entity.getUserEmail(),
                entity.getSubject(),
                entity.getSlaHours(),
                entity.getSlaDescription(),
                entity.getDescription(),
                entity.getAttachmentName(),
                entity.getAttachmentSize(),
                entity.getAttachmentType(),
                entity.getAttachmentData(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private SupportTicketEntity toEntity(SupportTicket domain) {
        if (domain == null) return null;
        SupportTicketEntity entity = new SupportTicketEntity();
        entity.setId(domain.getId());
        entity.setTicketCode(domain.getTicketCode());
        entity.setUserId(domain.getUserId());
        entity.setUserName(domain.getUserName());
        entity.setUserEmail(domain.getUserEmail());
        entity.setSubject(domain.getSubject());
        entity.setSlaHours(domain.getSlaHours());
        entity.setSlaDescription(domain.getSlaDescription());
        entity.setDescription(domain.getDescription());
        entity.setAttachmentName(domain.getAttachmentName());
        entity.setAttachmentSize(domain.getAttachmentSize());
        entity.setAttachmentType(domain.getAttachmentType());
        entity.setAttachmentData(domain.getAttachmentData());
        entity.setStatus(domain.getStatus() != null ? domain.getStatus() : "EM_ATENDIMENTO");
        entity.setCreatedAt(domain.getCreatedAt() != null ? domain.getCreatedAt() : OffsetDateTime.now());
        entity.setUpdatedAt(domain.getUpdatedAt() != null ? domain.getUpdatedAt() : OffsetDateTime.now());
        return entity;
    }
}
