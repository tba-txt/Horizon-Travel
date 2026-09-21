package br.com.horizon.horizon_api.domain.port;
import br.com.horizon.horizon_api.domain.model.*;
public interface UserProfileAttributePersistencePort {
    void deleteByUserId(Long userId); void saveAll(java.util.List<UserProfileAttribute> attributes); java.util.List<UserProfileAttribute> getUserProfilesByUserId(Long userId);
}
