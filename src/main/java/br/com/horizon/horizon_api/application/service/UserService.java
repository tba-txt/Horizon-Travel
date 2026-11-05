package br.com.horizon.horizon_api.application.service;

import br.com.horizon.horizon_api.domain.model.User;
import br.com.horizon.horizon_api.domain.port.UserPersistencePort;
import br.com.horizon.horizon_api.domain.port.PasswordEncodePort;
import br.com.horizon.horizon_api.domain.exception.BusinessException;
import br.com.horizon.horizon_api.domain.exception.ResourceNotFoundException;
import br.com.horizon.horizon_api.domain.exception.InvalidOperationException;
import br.com.horizon.horizon_api.application.dto.request.UserRequest;
import br.com.horizon.horizon_api.application.dto.request.ChangePasswordRequest;
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
public class UserService implements CreateUserUseCase, GetUserByIdUseCase, GetUserByEmailUseCase, UpdateUserUseCase, DeactivateUserUseCase, UploadProfilePictureUseCase, ChangePasswordUseCase {
    
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
        if (request.getCpf() == null || request.getCpf().trim().isEmpty()) {
            throw new BusinessException("O CPF é obrigatório para cadastro.");
        }
        if (request.getBirthDate() == null) {
            throw new BusinessException("A data de nascimento é obrigatória para cadastro.");
        }
        String cleanCpf = request.getCpf().replaceAll("\\D", "");
        request.setCpf(cleanCpf);

        if (userPort.existsByEmail(request.getEmail())) {
            throw new br.com.horizon.horizon_api.domain.exception.ConflictException("E-mail já cadastrado.");
        }
        if (userPort.existsByCpf(cleanCpf)) {
            throw new br.com.horizon.horizon_api.domain.exception.ConflictException("CPF já cadastrado.");
        }
        User user = userMapper.toDomain(request);
        user.validateAge();
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone().trim());
        }
        if (request.getCountry() != null && !request.getCountry().trim().isEmpty()) {
            user.setCountry(request.getCountry().trim());
        } else {
            user.setCountry("Brasil");
        }
        if (user.getProfileImageUrl() == null || user.getProfileImageUrl().trim().isEmpty()) {
            user.setProfileImageUrl("assets/default-avatar.webp");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setAuthProvider("LOCAL");
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
        if (request.getCpf() != null && !request.getCpf().trim().isEmpty()) {
            String cleanCpf = request.getCpf().replaceAll("\\D", "");
            if (existingUser.getCpf() != null && !existingUser.getCpf().equals(cleanCpf)) {
                throw new BusinessException("O CPF já foi cadastrado e não pode ser alterado.");
            }
            if (existingUser.getCpf() == null) {
                if (userPort.existsByCpf(cleanCpf)) {
                    throw new br.com.horizon.horizon_api.domain.exception.ConflictException("CPF já cadastrado.");
                }
                existingUser.setCpf(cleanCpf);
            }
        }
        existingUser.setName(request.getName());
        existingUser.setSocialName(request.getSocialName());
        existingUser.setEmail(request.getEmail());
        if (request.getBirthDate() != null) {
            if (existingUser.getBirthDate() != null && !existingUser.getBirthDate().equals(request.getBirthDate())) {
                throw new BusinessException("A data de nascimento já foi cadastrada e não pode ser alterada.");
            }
            if (existingUser.getBirthDate() == null) {
                existingUser.setBirthDate(request.getBirthDate());
                existingUser.validateAge();
            }
        }
        if (request.getPhone() != null) {
            existingUser.setPhone(request.getPhone().trim());
        }
        if (request.getCountry() != null && !request.getCountry().trim().isEmpty()) {
            existingUser.setCountry(request.getCountry().trim());
        }
        if (request.getProfileImageUrl() != null && !request.getProfileImageUrl().trim().isEmpty()) {
            existingUser.setProfileImageUrl(request.getProfileImageUrl());
        }
        existingUser.setUpdatedAt(OffsetDateTime.now());

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
            User saved = userPort.save(existingUser);
            return enrichWithProfile(userMapper.toResponse(saved));
        } catch (Exception e) {
            throw new BusinessException("Falha ao realizar upload da foto de perfil: " + e.getMessage());
        }
    }

    @Override
    public void execute(Long userId, ChangePasswordRequest request) {
        User existingUser = userPort.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));

        if ("GOOGLE".equalsIgnoreCase(existingUser.getAuthProvider())) {
            throw new BusinessException("Sua conta está conectada via Google. A alteração de senha deve ser feita diretamente na sua conta Google.");
        }

        if (existingUser.getPasswordHash() == null || !passwordEncoder.matches(request.getCurrentPassword(), existingUser.getPasswordHash())) {
            throw new BusinessException("A senha atual informada está incorreta.");
        }

        if (request.getNewPassword() == null || request.getNewPassword().trim().length() < 6 || request.getNewPassword().length() > 100) {
            throw new BusinessException("A nova senha deve ter entre 6 e 100 caracteres.");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("A nova senha e a confirmação de senha não conferem.");
        }

        if (passwordEncoder.matches(request.getNewPassword(), existingUser.getPasswordHash())) {
            throw new BusinessException("A nova senha não pode ser idêntica à senha atual.");
        }

        existingUser.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        existingUser.setUpdatedAt(OffsetDateTime.now());
        userPort.save(existingUser);
    }
}
