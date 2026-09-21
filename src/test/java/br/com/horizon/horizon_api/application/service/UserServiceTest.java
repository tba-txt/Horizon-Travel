package br.com.horizon.horizon_api.application.service;

import br.com.horizon.horizon_api.application.dto.request.UserRequest;
import br.com.horizon.horizon_api.application.dto.response.UserResponse;
import br.com.horizon.horizon_api.application.mapper.UserMapper;
import br.com.horizon.horizon_api.domain.exception.BusinessException;
import br.com.horizon.horizon_api.domain.exception.InvalidOperationException;
import br.com.horizon.horizon_api.domain.exception.ResourceNotFoundException;
import br.com.horizon.horizon_api.domain.model.User;
import br.com.horizon.horizon_api.domain.port.PasswordEncodePort;
import br.com.horizon.horizon_api.domain.port.UserPersistencePort;
import br.com.horizon.horizon_api.domain.port.UserProfilePersistencePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @Mock
    private UserPersistencePort userPort;

    @Mock
    private UserProfilePersistencePort userProfilePort;

    @Mock
    private PasswordEncodePort passwordEncoder;

    @InjectMocks
    private UserService userService;

    private UserMapper userMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userMapper = new UserMapper();
        // Since UserService uses userMapper directly but we mocked it above, we should set it manually or use Spy
        userService = new UserService(userPort, passwordEncoder, userMapper, userProfilePort);
        when(userProfilePort.findByUserId(any())).thenReturn(Optional.empty());
    }

    @Test
    void shouldCreateUserSuccessfully() {
        UserRequest request = new UserRequest();
        request.setName("John");
        request.setEmail("john@test.com");
        request.setCpf("12345678901");
        request.setPassword("password123");

        when(userPort.existsByEmail(any())).thenReturn(false);
        when(userPort.existsByCpf(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed_pass");
        
        when(userPort.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        UserResponse response = userService.execute(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("John", response.getName());
        assertEquals("john@test.com", response.getEmail());
        assertTrue(response.getActive());
        assertNotNull(response.getCreatedAt());
        
        verify(passwordEncoder).encode("password123");
        verify(userPort).save(any(User.class));
    }

    @Test
    void shouldThrowExceptionWhenEmailExists() {
        UserRequest request = new UserRequest();
        request.setEmail("john@test.com");
        request.setPassword("password123");

        when(userPort.existsByEmail("john@test.com")).thenReturn(true);

        assertThrows(BusinessException.class, () -> userService.execute(request));
        verify(userPort, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenCpfExists() {
        UserRequest request = new UserRequest();
        request.setEmail("john@test.com");
        request.setCpf("12345678901");
        request.setPassword("password123");

        when(userPort.existsByEmail("john@test.com")).thenReturn(false);
        when(userPort.existsByCpf("12345678901")).thenReturn(true);

        assertThrows(BusinessException.class, () -> userService.execute(request));
    }

    @Test
    void shouldFindByIdExisting() {
        User user = new User();
        user.setId(1L);
        when(userPort.findById(1L)).thenReturn(Optional.of(user));

        UserResponse response = userService.execute(1L);
        assertEquals(1L, response.getId());
    }

    @Test
    void shouldThrowWhenFindByIdNotExisting() {
        when(userPort.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> userService.execute(1L));
    }

    @Test
    void shouldUpdateUserSuccessfully() {
        User existing = new User();
        existing.setId(1L);
        existing.setEmail("old@test.com");
        existing.setCpf("11111111111");

        UserRequest request = new UserRequest();
        request.setEmail("new@test.com");
        request.setCpf("11111111111");

        when(userPort.findById(1L)).thenReturn(Optional.of(existing));
        when(userPort.existsByEmail("new@test.com")).thenReturn(false);
        
        when(userPort.save(any(User.class))).thenReturn(existing);

        UserResponse response = userService.execute(1L, request);
        assertEquals("new@test.com", response.getEmail());
        verify(userPort).save(existing);
    }

    @Test
    void shouldDeactivateUserSuccessfully() {
        User existing = new User();
        existing.setId(1L);
        existing.setActive(true);

        when(userPort.findById(1L)).thenReturn(Optional.of(existing));
        when(userPort.save(any(User.class))).thenReturn(existing);

        userService.deactivate(1L);

        assertFalse(existing.getActive());
        verify(userPort).save(existing);
    }

    @Test
    void shouldThrowWhenDeactivatingAlreadyInactiveUser() {
        User existing = new User();
        existing.setId(1L);
        existing.setActive(false);

        when(userPort.findById(1L)).thenReturn(Optional.of(existing));

        assertThrows(InvalidOperationException.class, () -> userService.deactivate(1L));
        verify(userPort, never()).save(any());
    }
}
