package br.com.horizon.horizon_api.infrastructure.persistence.repository;

import br.com.horizon.horizon_api.infrastructure.persistence.entity.AnswerAttributeEntity;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.AnswerAttributeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnswerAttributeRepository extends JpaRepository<AnswerAttributeEntity, AnswerAttributeId> {
}
