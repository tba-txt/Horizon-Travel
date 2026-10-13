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
        if (page == null) page = 0;
        if (size == null) size = 100;
        List<PostEntity> entities = postRepo.findByPublishedTrue(org.springframework.data.domain.PageRequest.of(page, size));
        if (entities.isEmpty()) return new java.util.ArrayList<>();

        List<Long> postIds = entities.stream().map(PostEntity::getId).collect(Collectors.toList());

        // 1. Carrega todos os atributos de todos os posts em UMA ÚNICA consulta JOIN FETCH
        List<PostAttributeEntity> allAttrs = attrRepo.findByPost_IdIn(postIds);
        java.util.Map<Long, List<PostAttribute>> attrsByPost = allAttrs.stream().map(a -> {
            PostAttribute pa = new PostAttribute();
            pa.setPostId(a.getPost().getId());
            pa.setAttributeId(a.getAttribute().getId());
            pa.setAttributeName(a.getAttribute() != null ? a.getAttribute().getName() : null);
            pa.setWeight(a.getWeight());
            return pa;
        }).collect(Collectors.groupingBy(PostAttribute::getPostId));

        // 2. Carrega todas as opções de Isso ou Aquilo em UMA ÚNICA consulta SQL
        java.util.Map<Long, List<Post.PostOption>> optionsByPost = new java.util.HashMap<>();
        List<Long> thisOrThatIds = entities.stream()
            .filter(e -> "THIS_OR_THAT".equalsIgnoreCase(e.getPostType()))
            .map(PostEntity::getId)
            .collect(Collectors.toList());

        if (!thisOrThatIds.isEmpty()) {
            String inSql = String.join(",", java.util.Collections.nCopies(thisOrThatIds.size(), "?"));
            jdbcTemplate.query(
                "SELECT id, post_id, label, image_url, votes_count FROM post_options WHERE post_id IN (" + inSql + ") ORDER BY id",
                ps -> {
                    for (int i = 0; i < thisOrThatIds.size(); i++) {
                        ps.setLong(i + 1, thisOrThatIds.get(i));
                    }
                },
                rs -> {
                    Long postId = rs.getLong("post_id");
                    Post.PostOption opt = new Post.PostOption();
                    opt.setId(rs.getLong("id"));
                    opt.setLabel(rs.getString("label"));
                    opt.setImage(rs.getString("image_url"));
                    opt.setVotes(rs.getInt("votes_count"));
                    optionsByPost.computeIfAbsent(postId, k -> new java.util.ArrayList<>()).add(opt);
                }
            );
        }

        // 3. Monta os modelos em memória sem disparar nenhuma query adicional (N+1 eliminado)
        return entities.stream().map(e -> {
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
            p.setAttributes(attrsByPost.getOrDefault(e.getId(), new java.util.ArrayList<>()));
            p.setOptions(optionsByPost.getOrDefault(e.getId(), new java.util.ArrayList<>()));
            return p;
        }).collect(Collectors.toList());
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
