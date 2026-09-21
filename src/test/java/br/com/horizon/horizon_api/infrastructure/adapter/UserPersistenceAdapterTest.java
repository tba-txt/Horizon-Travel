package br.com.horizon.horizon_api.infrastructure.adapter;

import br.com.horizon.horizon_api.application.mapper.UserMapper;
import br.com.horizon.horizon_api.domain.model.User;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.UserEntity;
import br.com.horizon.horizon_api.infrastructure.persistence.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserPersistenceAdapterTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldFindById() {
        UserEntity entity = new UserEntity();
        entity.setId(10L);
        User domain = new User();
        domain.setId(10L);

        when(userRepository.findById(10L)).thenReturn(Optional.of(entity));
        when(userMapper.toDomain(entity)).thenReturn(domain);

        Optional<User> result = adapter.findById(10L);

        assertTrue(result.isPresent());
        assertEquals(10L, result.get().getId());
        verify(userRepository, times(1)).findById(10L);
    }
}
