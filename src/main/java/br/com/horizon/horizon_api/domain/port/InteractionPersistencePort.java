package br.com.horizon.horizon_api.domain.port;
import br.com.horizon.horizon_api.domain.model.*;
public interface InteractionPersistencePort {
    java.util.Optional<Interaction> findByUserIdAndPostId(Long userId, Long postId);
    Interaction save(Interaction interaction);
    java.util.List<Interaction> getByUserId(Long userId);
    long countByPostIdAndType(Long postId, br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.InteractionType type);
    void delete(Long interactionId);
}
