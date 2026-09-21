package br.com.horizon.horizon_api.infrastructure.persistence.repository;

import br.com.horizon.horizon_api.infrastructure.persistence.entity.QuestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionRepository extends JpaRepository<QuestionEntity, Long> {
    java.util.List<br.com.horizon.horizon_api.infrastructure.persistence.entity.QuestionEntity> findByQuiz_IdAndActiveTrue(Long quizId);
}
