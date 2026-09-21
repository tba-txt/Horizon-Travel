package br.com.horizon.horizon_api.domain.port;

import br.com.horizon.horizon_api.domain.model.UserProfile;
import java.util.Optional;

public interface UserProfilePersistencePort {
    Optional<UserProfile> findByUserId(Long userId);
    UserProfile save(UserProfile profile);
}
