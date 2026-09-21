package br.com.horizon.horizon_api.infrastructure.persistence.repository;

import br.com.horizon.horizon_api.infrastructure.persistence.entity.InteractionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InteractionRepository extends JpaRepository<InteractionEntity, Long> {
    java.util.Optional<br.com.horizon.horizon_api.infrastructure.persistence.entity.InteractionEntity> findByUser_IdAndPost_Id(Long userId, Long postId);
    java.util.List<br.com.horizon.horizon_api.infrastructure.persistence.entity.InteractionEntity> findByUser_Id(Long userId);
    long countByPost_IdAndInteractionType(Long postId, br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.InteractionType type);
}
