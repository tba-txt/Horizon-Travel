package br.com.horizon.horizon_api.application.service;
import br.com.horizon.horizon_api.domain.model.Post;
import br.com.horizon.horizon_api.domain.model.PostAttribute;
import br.com.horizon.horizon_api.domain.port.PostPersistencePort;
import br.com.horizon.horizon_api.domain.port.PostAttributePersistencePort;
import br.com.horizon.horizon_api.domain.exception.ResourceNotFoundException;
import br.com.horizon.horizon_api.application.dto.request.PostRequest;
import br.com.horizon.horizon_api.application.dto.response.PostResponseDTO;
import br.com.horizon.horizon_api.application.dto.response.PostAttributeDTO;
import br.com.horizon.horizon_api.application.usecase.*;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

import br.com.horizon.horizon_api.domain.port.InteractionPersistencePort;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.InteractionType;

import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService implements CreatePostUseCase, GetPostByIdUseCase, ListPublishedPostsUseCase, UpdatePostUseCase, PublishPostUseCase, UnpublishPostUseCase {
    private final PostPersistencePort postPort;
    private final PostAttributePersistencePort postAttrPort;
    private final InteractionPersistencePort interactionPort;
    
    private PostResponseDTO map(Post p, br.com.horizon.horizon_api.domain.model.Interaction userInteraction) {
        PostResponseDTO d = new PostResponseDTO();
        d.setId(p.getId());
        d.setTitle(p.getTitle());
        d.setCaption(p.getCaption());
        d.setImageUrl(p.getImageUrl());
        d.setPublished(p.getPublished());
        d.setCreatedAt(p.getCreatedAt());
        // Load attributes
        if (p.getAttributes() != null) {
            d.setAttributes(p.getAttributes().stream().map(a -> {
                PostAttributeDTO dto = new PostAttributeDTO();
                dto.setAttributeId(a.getAttributeId());
                dto.setAttributeName(a.getAttributeName());
                dto.setWeight(a.getWeight());
                return dto;
            }).collect(Collectors.toList()));
        }
        
        d.setLikesCount(p.getLikesCount());
        d.setDislikesCount(p.getDislikesCount());
        d.setPostType(p.getPostType());
        
        if (p.getOptions() != null && !p.getOptions().isEmpty()) {
            d.setThisOrThatOptions(p.getOptions().stream().map(o -> {
                PostResponseDTO.ThisOrThatOptionResponse dto = new PostResponseDTO.ThisOrThatOptionResponse();
                dto.setId(o.getId());
                dto.setLabel(o.getLabel());
                dto.setImage(o.getImage());
                dto.setVotes(o.getVotes());
                return dto;
            }).collect(Collectors.toList()));
        }
        
        if (userInteraction != null) {
            d.setUserInteraction(userInteraction.getInteractionType());
            d.setUserOptionId(userInteraction.getOptionId());
        }
        
        return d;
    }

    private br.com.horizon.horizon_api.domain.model.Interaction getUserInteraction(Long postId) {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Long) {
            Long userId = (Long) auth.getPrincipal();
            return interactionPort.findByUserIdAndPostId(userId, postId).orElse(null);
        }
        return null;
    }
    
    @Override
    @Transactional
    public PostResponseDTO execute(PostRequest req) {
        Post p = new Post();
        p.setTitle(req.getTitle());
        p.setCaption(req.getCaption());
        p.setImageUrl(req.getImageUrl());
        p.setPublished(req.getPublished() != null ? req.getPublished() : false);
        p.setCreatedAt(OffsetDateTime.now());
        p.setUpdatedAt(OffsetDateTime.now());
        return map(postPort.save(p), null);
    }
    
    @Override
    public PostResponseDTO execute(Long id) {
        return map(postPort.findById(id).orElseThrow(() -> new ResourceNotFoundException("Post não encontrado.")), getUserInteraction(id));
    }
    
    @Override
    public List<PostResponseDTO> execute(Integer page, Integer size) {
        List<Post> posts = postPort.findPublished(page, size);
        java.util.Map<Long, br.com.horizon.horizon_api.domain.model.Interaction> userInteractions = new java.util.HashMap<>();
        
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Long) {
            Long userId = (Long) auth.getPrincipal();
            // Fetch all interactions for the user in one query
            List<br.com.horizon.horizon_api.domain.model.Interaction> interactions = interactionPort.getByUserId(userId);
            for (br.com.horizon.horizon_api.domain.model.Interaction i : interactions) {
                userInteractions.put(i.getPostId(), i);
            }
        }
        
        return posts.stream().map(p -> map(p, userInteractions.get(p.getId()))).collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public PostResponseDTO execute(Long id, PostRequest req) {
        Post p = postPort.findById(id).orElseThrow(() -> new ResourceNotFoundException("Post nǜo encontrado."));
        p.setTitle(req.getTitle());
        p.setCaption(req.getCaption());
        p.setImageUrl(req.getImageUrl());
        if (req.getPublished() != null) p.setPublished(req.getPublished());
        p.setUpdatedAt(OffsetDateTime.now());
        return map(postPort.save(p), getUserInteraction(id));
    }
    
    @Override
    @Transactional
    public void executePublish(Long id) {
        Post p = postPort.findById(id).orElseThrow(() -> new ResourceNotFoundException("Post nǜo encontrado."));
        p.setPublished(true);
        p.setUpdatedAt(OffsetDateTime.now());
        postPort.save(p);
    }
    
    @Override
    @Transactional
    public void executeUnpublish(Long id) {
        Post p = postPort.findById(id).orElseThrow(() -> new ResourceNotFoundException("Post não encontrado."));
        p.setPublished(false);
        p.setUpdatedAt(OffsetDateTime.now());
        postPort.save(p);
    }
}
