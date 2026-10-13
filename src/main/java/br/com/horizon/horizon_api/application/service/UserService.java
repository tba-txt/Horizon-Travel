package br.com.horizon.horizon_api.application.service;

import br.com.horizon.horizon_api.domain.model.User;
import br.com.horizon.horizon_api.domain.port.UserPersistencePort;
import br.com.horizon.horizon_api.domain.port.PasswordEncodePort;
import br.com.horizon.horizon_api.domain.exception.BusinessException;
import br.com.horizon.horizon_api.domain.exception.ResourceNotFoundException;
import br.com.horizon.horizon_api.domain.exception.InvalidOperationException;
import br.com.horizon.horizon_api.application.dto.request.UserRequest;
import br.com.horizon.horizon_api.application.dto.response.UserResponse;
import br.com.horizon.horizon_api.application.mapper.UserMapper;
import br.com.horizon.horizon_api.domain.port.UserProfilePersistencePort;
import br.com.horizon.horizon_api.domain.model.UserProfile;
import br.com.horizon.horizon_api.application.usecase.*;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class UserService implements CreateUserUseCase, GetUserByIdUseCase, GetUserByEmailUseCase, UpdateUserUseCase, DeactivateUserUseCase, UploadProfilePictureUseCase {
    
    private final UserPersistencePort userPort;
    private final PasswordEncodePort passwordEncoder;
    private final UserMapper userMapper;
    private final UserProfilePersistencePort userProfilePort;
    private final br.com.horizon.horizon_api.infrastructure.messaging.CloudinaryServiceAdapter cloudinaryServiceAdapter;

    private UserResponse enrichWithProfile(UserResponse response) {
        userProfilePort.findByUserId(response.getId()).ifPresent(p -> {
            response.setBudgetPerPerson(p.getBudgetPerPerson());
        });
        return response;
    }

    @Override
    public UserResponse execute(UserRequest request) {
        if (request.getPassword() == null || request.getPassword().trim().isEmpty() || request.getPassword().length() < 6 || request.getPassword().length() > 100) {
            throw new BusinessException("A senha deve ter entre 6 e 100 caracteres.");
        }
        if (userPort.existsByEmail(request.getEmail())) {
            throw new br.com.horizon.horizon_api.domain.exception.ConflictException("E-mail já cadastrado.");
        }
        if (userPort.existsByCpf(request.getCpf())) {
            throw new br.com.horizon.horizon_api.domain.exception.ConflictException("CPF já cadastrado.");
        }
        User user = userMapper.toDomain(request);
        user.validateAge();
        if (user.getProfileImageUrl() == null || user.getProfileImageUrl().trim().isEmpty()) {
            user.setProfileImageUrl("assets/default-avatar.webp");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setActive(true);
        user.setCreatedAt(OffsetDateTime.now());
        user.setUpdatedAt(OffsetDateTime.now());

        User savedUser = userPort.save(user);
        return enrichWithProfile(userMapper.toResponse(savedUser));
    }

    @Override
    public UserResponse execute(Long id) {
        User user = userPort.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
        return enrichWithProfile(userMapper.toResponse(user));
    }

    @Override
    public UserResponse execute(String email) {
        User user = userPort.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
        return enrichWithProfile(userMapper.toResponse(user));
    }

    @Override
    public UserResponse execute(Long id, UserRequest request) {
        User existingUser = userPort.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));

        if (!existingUser.getEmail().equalsIgnoreCase(request.getEmail()) && userPort.existsByEmail(request.getEmail())) {
            throw new br.com.horizon.horizon_api.domain.exception.ConflictException("E-mail já cadastrado.");
        }
        if (!existingUser.getCpf().equals(request.getCpf()) && userPort.existsByCpf(request.getCpf())) {
            throw new br.com.horizon.horizon_api.domain.exception.ConflictException("CPF já cadastrado.");
        }
        existingUser.setName(request.getName());
        existingUser.setSocialName(request.getSocialName());
        existingUser.setEmail(request.getEmail());
        existingUser.setCpf(request.getCpf());
        existingUser.setBirthDate(request.getBirthDate());
        existingUser.validateAge();
        existingUser.setProfileImageUrl(request.getProfileImageUrl());
        existingUser.setUpdatedAt(OffsetDateTime.now());
        // password hash is NOT updated here as per constraints
        // id, active, createdAt are kept intact

        User savedUser = userPort.save(existingUser);
        return enrichWithProfile(userMapper.toResponse(savedUser));
    }

    @Override
    public void deactivate(Long id) {
        User existingUser = userPort.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
            
        if (!existingUser.getActive()) {
            throw new InvalidOperationException("Usuário já está desativado.");
        }

        existingUser.setActive(false);
        existingUser.setUpdatedAt(OffsetDateTime.now());
        userPort.save(existingUser);
    }

    @Override
    public UserResponse execute(Long userId, org.springframework.web.multipart.MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Nenhum arquivo de imagem foi enviado.");
        }

        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equalsIgnoreCase("image/jpeg") &&
                                    !contentType.equalsIgnoreCase("image/png") &&
                                    !contentType.equalsIgnoreCase("image/webp") &&
                                    !contentType.equalsIgnoreCase("image/jpg"))) {
            throw new BusinessException("Formato inválido. Apenas imagens (JPG, PNG, WEBP) são permitidas.");
        }

        if (file.getSize() > 10 * 1024 * 1024) {
            throw new BusinessException("A foto de perfil não pode ultrapassar 10MB.");
        }

        User existingUser = userPort.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
        
        try {
            String imageUrl = cloudinaryServiceAdapter.uploadImage(file);
            existingUser.setProfileImageUrl(imageUrl);
            existingUser.setUpdatedAt(OffsetDateTime.now());
            User savedUser = userPort.save(existingUser);
            return enrichWithProfile(userMapper.toResponse(savedUser));
        } catch (Exception e) {
            throw new BusinessException("Erro ao fazer upload da imagem: " + e.getMessage());
        }
    }
}
