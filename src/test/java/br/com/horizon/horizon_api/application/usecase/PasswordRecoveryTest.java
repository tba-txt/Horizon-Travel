package br.com.horizon.horizon_api.application.usecase;

import br.com.horizon.horizon_api.application.dto.request.ForgotPasswordRequest;
import br.com.horizon.horizon_api.application.dto.request.ResetPasswordRequest;
import br.com.horizon.horizon_api.domain.exception.BusinessException;
import br.com.horizon.horizon_api.domain.port.EmailPort;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.PasswordResetTokenEntity;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.UserEntity;
import br.com.horizon.horizon_api.infrastructure.persistence.repository.PasswordResetTokenRepository;
import br.com.horizon.horizon_api.infrastructure.persistence.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordRecoveryTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private EmailPort emailPort;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ForgotPasswordUseCase forgotPasswordUseCase;

    @InjectMocks
    private ResetPasswordUseCase resetPasswordUseCase;

    private UserEntity user;
    private PasswordResetTokenEntity tokenEntity;

    @BeforeEach
    void setUp() {
        user = new UserEntity();
        user.setId(1L);
        user.setEmail("test@test.com");
        user.setName("Test User");
        user.setPasswordHash("oldHash");

        tokenEntity = new PasswordResetTokenEntity();
        tokenEntity.setUser(user);
        tokenEntity.setTokenHash("mock-token");
        tokenEntity.setUsed(false);
        tokenEntity.setExpiresAt(OffsetDateTime.now().plusHours(1));
    }

    @Test
    void testForgotPasswordSuccess() {
        ForgotPasswordRequest req = new ForgotPasswordRequest();
        req.setEmail("test@test.com");

        when(userRepository.findByEmailIgnoreCase("test@test.com")).thenReturn(Optional.of(user));

        forgotPasswordUseCase.execute(req);

        verify(tokenRepository).save(any(PasswordResetTokenEntity.class));
        verify(emailPort).sendEmail(eq("test@test.com"), anyString(), anyString());
    }

    @Test
    void testResetPasswordSuccess() {
        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setToken("mock-token");
        req.setNewPassword("newPass123");

        when(tokenRepository.findByTokenHash("mock-token")).thenReturn(Optional.of(tokenEntity));
        when(passwordEncoder.encode("newPass123")).thenReturn("newHash");

        resetPasswordUseCase.execute(req);

        assertEquals("newHash", user.getPasswordHash());
        assertTrue(tokenEntity.getUsed());
        verify(userRepository).save(user);
        verify(tokenRepository).save(tokenEntity);
    }

    @Test
    void testResetPasswordExpiredToken() {
        tokenEntity.setExpiresAt(OffsetDateTime.now().minusHours(1));
        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setToken("mock-token");
        req.setNewPassword("newPass123");

        when(tokenRepository.findByTokenHash("mock-token")).thenReturn(Optional.of(tokenEntity));

        assertThrows(BusinessException.class, () -> resetPasswordUseCase.execute(req));
        verify(userRepository, never()).save(any());
    }

    @Test
    void testResetPasswordUsedToken() {
        tokenEntity.setUsed(true);
        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setToken("mock-token");
        req.setNewPassword("newPass123");

        when(tokenRepository.findByTokenHash("mock-token")).thenReturn(Optional.of(tokenEntity));

        assertThrows(BusinessException.class, () -> resetPasswordUseCase.execute(req));
        verify(userRepository, never()).save(any());
    }
}