package br.com.horizon.horizon_api.application.service;

import br.com.horizon.horizon_api.application.dto.request.LoginRequest;
import br.com.horizon.horizon_api.application.dto.response.LoginResponse;
import br.com.horizon.horizon_api.domain.exception.InvalidOperationException;
import br.com.horizon.horizon_api.domain.exception.UnauthorizedException;
import br.com.horizon.horizon_api.domain.model.User;
import br.com.horizon.horizon_api.domain.port.LoginAttemptPort;
import br.com.horizon.horizon_api.domain.port.PasswordEncodePort;
import br.com.horizon.horizon_api.domain.port.TokenPort;
import br.com.horizon.horizon_api.domain.port.UserPersistencePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserPersistencePort userPort;

    @Mock
    private PasswordEncodePort passwordEncodePort;

    @Mock
    private TokenPort tokenPort;

    @Mock
    private LoginAttemptPort loginAttemptPort;

    @InjectMocks
    private AuthService authService;

    private User activeUser;

    @BeforeEach
    void setUp() {
        activeUser = new User();
        activeUser.setId(1L);
        activeUser.setEmail("teste@horizon.com");
        activeUser.setPasswordHash("hashed_secret");
        activeUser.setActive(true);
    }

    @Test
    void shouldLoginSuccessfully() {
        LoginRequest request = new LoginRequest();
        request.setEmail("teste@horizon.com");
        request.setPassword("senha123");

        when(loginAttemptPort.isBlocked("teste@horizon.com")).thenReturn(false);
        when(userPort.findByEmail("teste@horizon.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncodePort.matches("senha123", "hashed_secret")).thenReturn(true);
        when(tokenPort.generateToken(activeUser)).thenReturn("fake-jwt-token");

        LoginResponse response = authService.execute(request);

        assertNotNull(response);
        assertEquals("fake-jwt-token", response.getToken());
        verify(loginAttemptPort).loginSucceeded("teste@horizon.com");
    }

    @Test
    void shouldRejectWhenBlockedByBruteForce() {
        LoginRequest request = new LoginRequest();
        request.setEmail("blocked@horizon.com");
        request.setPassword("senha123");

        when(loginAttemptPort.isBlocked("blocked@horizon.com")).thenReturn(true);

        assertThrows(UnauthorizedException.class, () -> authService.execute(request));
        verify(userPort, never()).findByEmail(anyString());
    }

    @Test
    void shouldRegisterFailureWhenPasswordIsIncorrect() {
        LoginRequest request = new LoginRequest();
        request.setEmail("teste@horizon.com");
        request.setPassword("wrongpass");

        when(loginAttemptPort.isBlocked("teste@horizon.com")).thenReturn(false);
        when(userPort.findByEmail("teste@horizon.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncodePort.matches("wrongpass", "hashed_secret")).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> authService.execute(request));
        verify(loginAttemptPort).loginFailed("teste@horizon.com");
    }

    @Test
    void shouldRejectDeactivatedUser() {
        activeUser.setActive(false);
        LoginRequest request = new LoginRequest();
        request.setEmail("teste@horizon.com");
        request.setPassword("senha123");

        when(loginAttemptPort.isBlocked("teste@horizon.com")).thenReturn(false);
        when(userPort.findByEmail("teste@horizon.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncodePort.matches("senha123", "hashed_secret")).thenReturn(true);

        assertThrows(InvalidOperationException.class, () -> authService.execute(request));
    }
}
