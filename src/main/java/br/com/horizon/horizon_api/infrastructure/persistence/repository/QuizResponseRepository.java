package br.com.horizon.horizon_api.infrastructure.persistence.repository;

import br.com.horizon.horizon_api.infrastructure.persistence.entity.QuizResponseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuizResponseRepository extends JpaRepository<QuizResponseEntity, Long> {
}
