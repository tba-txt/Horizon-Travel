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
public class UserService implements CreateUserUseCase, GetUserByIdUseCase, GetUserByEmailUseCase, UpdateUserUseCase, DeactivateUserUseCase {
    
    private final UserPersistencePort userPort;
    private final PasswordEncodePort passwordEncoder;
    private final UserMapper userMapper;
    private final UserProfilePersistencePort userProfilePort;

    private UserResponse enrichWithProfile(UserResponse response) {
        userProfilePort.findByUserId(response.getId()).ifPresent(p -> {
            response.setBudgetPerPerson(p.getBudgetPerPerson());
        });
        return response;
    }

    @Override
    public UserResponse execute(UserRequest request) {
        if (request.getPassword() == null || request.getPassword().trim().isEmpty() || request.getPassword().length() < 6) {
            throw new BusinessException("A senha deve ter pelo menos 6 caracteres.");
        }
        if (userPort.existsByEmail(request.getEmail())) {
            throw new br.com.horizon.horizon_api.domain.exception.ConflictException("E-mail já cadastrado.");
        }
        if (userPort.existsByCpf(request.getCpf())) {
            throw new br.com.horizon.horizon_api.domain.exception.ConflictException("CPF já cadastrado.");
        }
        if (request.getBirthDate() != null && java.time.temporal.ChronoUnit.YEARS.between(request.getBirthDate(), java.time.LocalDate.now()) < 1) {
            throw new BusinessException("O usuário deve ter pelo menos 1 ano de idade.");
        }

        User user = userMapper.toDomain(request);
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
        if (request.getBirthDate() != null && java.time.temporal.ChronoUnit.YEARS.between(request.getBirthDate(), java.time.LocalDate.now()) < 1) {
            throw new BusinessException("O usuário deve ter pelo menos 1 ano de idade.");
        }

        existingUser.setName(request.getName());
        existingUser.setSocialName(request.getSocialName());
        existingUser.setEmail(request.getEmail());
        existingUser.setCpf(request.getCpf());
        existingUser.setBirthDate(request.getBirthDate());
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
}
