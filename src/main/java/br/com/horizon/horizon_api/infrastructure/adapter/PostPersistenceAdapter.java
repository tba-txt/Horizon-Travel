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
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    
    private Post map(PostEntity e) {
        Post p = new Post();
        p.setId(e.getId());
        p.setTitle(e.getTitle());
        p.setCaption(e.getCaption());
        p.setImageUrl(e.getImageUrl());
        p.setPublished(e.getPublished());
        p.setCreatedAt(e.getCreatedAt());
        p.setUpdatedAt(e.getUpdatedAt());
        p.setLikesCount(e.getLikesCount() != null ? e.getLikesCount() : 0L);
        p.setDislikesCount(e.getDislikesCount() != null ? e.getDislikesCount() : 0L);
        p.setPostType(e.getPostType());
        
        if (e.getAttributes() != null) {
            List<PostAttribute> attrs = e.getAttributes().stream().map(a -> {
                PostAttribute pa = new PostAttribute();
                pa.setPostId(a.getPost().getId());
                pa.setAttributeId(a.getAttribute().getId());
                pa.setAttributeName(a.getAttribute().getName());
                pa.setWeight(a.getWeight());
                return pa;
            }).collect(Collectors.toList());
            p.setAttributes(attrs);
        }

        if ("THIS_OR_THAT".equalsIgnoreCase(e.getPostType()) && e.getId() != null) {
            List<Post.PostOption> options = jdbcTemplate.query(
                "SELECT id, label, image_url, votes_count FROM post_options WHERE post_id = ? ORDER BY id",
                (rs, rowNum) -> {
                    Post.PostOption opt = new Post.PostOption();
                    opt.setId(rs.getLong("id"));
                    opt.setLabel(rs.getString("label"));
                    opt.setImage(rs.getString("image_url"));
                    opt.setVotes(rs.getInt("votes_count"));
                    return opt;
                },
                e.getId()
            );
            p.setOptions(options);
        }
        
        return p;
    }
    
    @Override
    public Post save(Post p) {
        PostEntity e = new PostEntity();
        if (p.getId() != null) {
            e = postRepo.findById(p.getId()).orElse(new PostEntity());
        }
        e.setTitle(p.getTitle());
        e.setCaption(p.getCaption());
        e.setImageUrl(p.getImageUrl());
        e.setPublished(p.getPublished());
        e.setCreatedAt(p.getCreatedAt());
        e.setUpdatedAt(p.getUpdatedAt());
        e.setPostType(p.getPostType());
        if (p.getLikesCount() != null) e.setLikesCount(p.getLikesCount());
        if (p.getDislikesCount() != null) e.setDislikesCount(p.getDislikesCount());
        return map(postRepo.save(e));
    }
    
    @Override
    public Optional<Post> findById(Long id) {
        return postRepo.findById(id).map(this::map);
    }
    
    @Override
    public List<Post> findPublished(Integer page, Integer size) {
        return postRepo.findByPublishedTrue(org.springframework.data.domain.PageRequest.of(page != null ? page : 0, size != null ? size : 100)).stream().map(this::map).collect(Collectors.toList());
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
