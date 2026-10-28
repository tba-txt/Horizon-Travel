package br.com.horizon.horizon_api.infrastructure.persistence.repository;

import br.com.horizon.horizon_api.infrastructure.persistence.entity.SupportTicketEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicketEntity, Long> {

    List<SupportTicketEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<SupportTicketEntity> findByUserEmailOrderByCreatedAtDesc(String userEmail);

    Optional<SupportTicketEntity> findByTicketCode(String ticketCode);
}
