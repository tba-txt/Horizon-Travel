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
@Transactional
public class PostService implements CreatePostUseCase, GetPostByIdUseCase, ListPublishedPostsUseCase, UpdatePostUseCase, PublishPostUseCase, UnpublishPostUseCase {
    private final PostPersistencePort postPort;
    private final PostAttributePersistencePort postAttrPort;
    private final InteractionPersistencePort interactionPort;
    
    private PostResponseDTO map(Post p) {
        PostResponseDTO d = new PostResponseDTO();
        d.setId(p.getId());
        d.setTitle(p.getTitle());
        d.setCaption(p.getCaption());
        d.setImageUrl(p.getImageUrl());
        d.setDestinationId(p.getDestinationId());
        d.setPublished(p.getPublished());
        d.setCreatedAt(p.getCreatedAt());
        // Load attributes
        List<PostAttribute> attrs = postAttrPort.findByPostId(p.getId());
        d.setAttributes(attrs.stream().map(a -> {
            PostAttributeDTO dto = new PostAttributeDTO();
            dto.setAttributeId(a.getAttributeId());
            dto.setAttributeName(a.getAttributeName());
            dto.setWeight(a.getWeight());
            return dto;
        }).collect(Collectors.toList()));
        
        d.setLikesCount(interactionPort.countByPostIdAndType(p.getId(), InteractionType.LIKE));
        d.setDislikesCount(interactionPort.countByPostIdAndType(p.getId(), InteractionType.DISLIKE));
        
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Long) {
            Long userId = (Long) auth.getPrincipal();
            interactionPort.findByUserIdAndPostId(userId, p.getId())
                .ifPresent(i -> d.setUserInteraction(i.getInteractionType()));
        }
        
        return d;
    }
    
    @Override
    public PostResponseDTO execute(PostRequest req) {
        Post p = new Post();
        p.setTitle(req.getTitle());
        p.setCaption(req.getCaption());
        p.setImageUrl(req.getImageUrl());
        p.setDestinationId(req.getDestinationId());
        p.setPublished(req.getPublished() != null ? req.getPublished() : false);
        p.setCreatedAt(OffsetDateTime.now());
        p.setUpdatedAt(OffsetDateTime.now());
        return map(postPort.save(p));
    }
    
    @Override
    public PostResponseDTO execute(Long id) {
        return map(postPort.findById(id).orElseThrow(() -> new ResourceNotFoundException("Post não encontrado.")));
    }
    
    @Override
    public List<PostResponseDTO> execute() {
        return postPort.findPublished().stream().map(this::map).collect(Collectors.toList());
    }
    
    @Override
    public PostResponseDTO execute(Long id, PostRequest req) {
        Post p = postPort.findById(id).orElseThrow(() -> new ResourceNotFoundException("Post não encontrado."));
        p.setTitle(req.getTitle());
        p.setCaption(req.getCaption());
        p.setImageUrl(req.getImageUrl());
        p.setDestinationId(req.getDestinationId());
        if (req.getPublished() != null) p.setPublished(req.getPublished());
        p.setUpdatedAt(OffsetDateTime.now());
        return map(postPort.save(p));
    }
    
    @Override
    public void executePublish(Long id) {
        Post p = postPort.findById(id).orElseThrow(() -> new ResourceNotFoundException("Post não encontrado."));
        p.setPublished(true);
        p.setUpdatedAt(OffsetDateTime.now());
        postPort.save(p);
    }
    
    @Override
    public void executeUnpublish(Long id) {
        Post p = postPort.findById(id).orElseThrow(() -> new ResourceNotFoundException("Post não encontrado."));
        p.setPublished(false);
        p.setUpdatedAt(OffsetDateTime.now());
        postPort.save(p);
    }
}
