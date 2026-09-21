package br.com.horizon.horizon_api.infrastructure.adapter;
import br.com.horizon.horizon_api.domain.model.Interaction;
import br.com.horizon.horizon_api.domain.port.InteractionPersistencePort;
import br.com.horizon.horizon_api.infrastructure.persistence.repository.InteractionRepository;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.InteractionEntity;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.UserEntity;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.PostEntity;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class InteractionPersistenceAdapter implements InteractionPersistencePort {
    private final InteractionRepository repo;
    
    private Interaction map(InteractionEntity e) {
        Interaction i = new Interaction();
        i.setId(e.getId());
        i.setUserId(e.getUser().getId());
        i.setPostId(e.getPost().getId());
        i.setInteractionType(e.getInteractionType());
        i.setCreatedAt(e.getCreatedAt());
        return i;
    }
    
    @Override
    public Optional<Interaction> findByUserIdAndPostId(Long userId, Long postId) {
        return repo.findByUser_IdAndPost_Id(userId, postId).map(this::map);
    }
    
    @Override
    public Interaction save(Interaction i) {
        InteractionEntity e = new InteractionEntity();
        e.setId(i.getId());
        UserEntity u = new UserEntity(); u.setId(i.getUserId()); e.setUser(u);
        PostEntity p = new PostEntity(); p.setId(i.getPostId()); e.setPost(p);
        e.setInteractionType(i.getInteractionType());
        e.setCreatedAt(i.getCreatedAt());
        return map(repo.save(e));
    }
    
    @Override
    public List<Interaction> getByUserId(Long userId) {
        return repo.findByUser_Id(userId).stream().map(this::map).collect(Collectors.toList());
    }
    
    @Override
    public long countByPostIdAndType(Long postId, br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.InteractionType type) {
        return repo.countByPost_IdAndInteractionType(postId, type);
    }
    
    @Override
    public void delete(Long interactionId) {
        repo.deleteById(interactionId);
    }
}
