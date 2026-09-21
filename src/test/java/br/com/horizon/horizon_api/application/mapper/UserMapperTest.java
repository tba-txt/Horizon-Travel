package br.com.horizon.horizon_api.application.mapper;

import br.com.horizon.horizon_api.domain.model.User;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.UserEntity;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UserMapperTest {

    private final UserMapper mapper = new UserMapper();

    @Test
    void shouldMapEntityToDomain() {
        UserEntity entity = new UserEntity();
        entity.setId(1L);
        
        User domain = mapper.toDomain(entity);
        
        assertNotNull(domain);
        assertEquals(1L, domain.getId());
    }

    @Test
    void shouldMapDomainToEntity() {
        User domain = new User();
        domain.setId(2L);
        
        UserEntity entity = mapper.toEntity(domain);
        
        assertNotNull(entity);
        assertEquals(2L, entity.getId());
    }
}
