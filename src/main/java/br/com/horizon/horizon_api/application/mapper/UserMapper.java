package br.com.horizon.horizon_api.application.mapper;

import br.com.horizon.horizon_api.domain.model.User;
import br.com.horizon.horizon_api.application.dto.request.UserRequest;
import br.com.horizon.horizon_api.application.dto.response.UserResponse;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    
    public User toDomain(UserEntity entity) {
        if (entity == null) return null;
        User domain = new User();
        domain.setId(entity.getId());
        domain.setName(entity.getName());
        domain.setSocialName(entity.getSocialName());
        domain.setEmail(entity.getEmail());
        domain.setCpf(entity.getCpf());
        domain.setPasswordHash(entity.getPasswordHash());
        domain.setBirthDate(entity.getBirthDate());
        domain.setProfileImageUrl(entity.getProfileImageUrl());
        domain.setActive(entity.getActive());
        domain.setCreatedAt(entity.getCreatedAt());
        domain.setUpdatedAt(entity.getUpdatedAt());
        return domain;
    }
    
    public UserEntity toEntity(User domain) {
        if (domain == null) return null;
        UserEntity entity = new UserEntity();
        entity.setId(domain.getId());
        entity.setName(domain.getName());
        entity.setSocialName(domain.getSocialName());
        entity.setEmail(domain.getEmail());
        entity.setCpf(domain.getCpf());
        entity.setPasswordHash(domain.getPasswordHash());
        entity.setBirthDate(domain.getBirthDate());
        entity.setProfileImageUrl(domain.getProfileImageUrl());
        entity.setActive(domain.getActive());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
    
    public User toDomain(UserRequest request) {
        if (request == null) return null;
        User domain = new User();
        domain.setName(request.getName());
        domain.setSocialName(request.getSocialName());
        domain.setEmail(request.getEmail());
        domain.setCpf(request.getCpf());
        domain.setBirthDate(request.getBirthDate());
        domain.setProfileImageUrl(request.getProfileImageUrl());
        // password hash is handled in service
        return domain;
    }
    
    public UserResponse toResponse(User domain) {
        if (domain == null) return null;
        UserResponse response = new UserResponse();
        response.setId(domain.getId());
        response.setName(domain.getName());
        response.setSocialName(domain.getSocialName());
        response.setEmail(domain.getEmail());
        response.setCpf(domain.getCpf());
        response.setBirthDate(domain.getBirthDate());
        response.setProfileImageUrl(domain.getProfileImageUrl());
        response.setActive(domain.getActive());
        response.setCreatedAt(domain.getCreatedAt());
        response.setUpdatedAt(domain.getUpdatedAt());
        return response;
    }
}
