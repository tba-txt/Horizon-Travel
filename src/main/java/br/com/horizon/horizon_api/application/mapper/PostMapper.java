package br.com.horizon.horizon_api.application.mapper;

import br.com.horizon.horizon_api.domain.model.Post;
import br.com.horizon.horizon_api.application.dto.request.PostRequest;
import br.com.horizon.horizon_api.application.dto.response.PostResponse;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.PostEntity;
import org.springframework.stereotype.Component;

@Component
public class PostMapper {
    
    public Post toDomain(PostEntity entity) {
        if (entity == null) return null;
        Post domain = new Post();
        domain.setId(entity.getId());
        return domain;
    }
    
    public PostEntity toEntity(Post domain) {
        if (domain == null) return null;
        PostEntity entity = new PostEntity();
        entity.setId(domain.getId());
        return entity;
    }
    
    public Post toDomain(PostRequest request) {
        if (request == null) return null;
        return new Post();
    }
    
    public PostResponse toResponse(Post domain) {
        if (domain == null) return null;
        PostResponse response = new PostResponse();
        response.setId(domain.getId());
        return response;
    }
}
