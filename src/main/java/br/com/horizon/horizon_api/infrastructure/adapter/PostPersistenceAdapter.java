package br.com.horizon.horizon_api.infrastructure.adapter;
import br.com.horizon.horizon_api.domain.model.Post;
import br.com.horizon.horizon_api.domain.model.PostAttribute;
import br.com.horizon.horizon_api.domain.port.PostPersistencePort;
import br.com.horizon.horizon_api.domain.port.PostAttributePersistencePort;
import br.com.horizon.horizon_api.infrastructure.persistence.repository.PostRepository;
import br.com.horizon.horizon_api.infrastructure.persistence.repository.PostAttributeRepository;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.PostEntity;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.PostAttributeEntity;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.DestinationEntity;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PostPersistenceAdapter implements PostPersistencePort, PostAttributePersistencePort {
    private final PostRepository postRepo;
    private final PostAttributeRepository attrRepo;
    
    private Post map(PostEntity e) {
        Post p = new Post();
        p.setId(e.getId());
        p.setTitle(e.getTitle());
        p.setCaption(e.getCaption());
        p.setImageUrl(e.getImageUrl());
        if (e.getDestination() != null) p.setDestinationId(e.getDestination().getId());
        p.setPublished(e.getPublished());
        p.setCreatedAt(e.getCreatedAt());
        p.setUpdatedAt(e.getUpdatedAt());
        return p;
    }
    
    @Override
    public Post save(Post p) {
        PostEntity e = new PostEntity();
        e.setId(p.getId());
        e.setTitle(p.getTitle());
        e.setCaption(p.getCaption());
        e.setImageUrl(p.getImageUrl());
        if (p.getDestinationId() != null) {
            DestinationEntity d = new DestinationEntity(); d.setId(p.getDestinationId()); e.setDestination(d);
        }
        e.setPublished(p.getPublished());
        e.setCreatedAt(p.getCreatedAt());
        e.setUpdatedAt(p.getUpdatedAt());
        return map(postRepo.save(e));
    }
    
    @Override
    public Optional<Post> findById(Long id) {
        return postRepo.findById(id).map(this::map);
    }
    
    @Override
    public List<Post> findPublished() {
        return postRepo.findByPublishedTrue().stream().map(this::map).collect(Collectors.toList());
    }
    
    @Override
    public List<PostAttribute> findByPostId(Long postId) {
        return attrRepo.findByPost_Id(postId).stream().map(e -> {
            PostAttribute pa = new PostAttribute();
            pa.setPostId(e.getPost().getId());
            pa.setAttributeId(e.getAttribute().getId());
            pa.setAttributeName(e.getAttribute().getName());
            pa.setWeight(e.getWeight());
            return pa;
        }).collect(Collectors.toList());
    }
}
