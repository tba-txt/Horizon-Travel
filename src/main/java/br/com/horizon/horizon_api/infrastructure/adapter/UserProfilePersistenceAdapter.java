package br.com.horizon.horizon_api.infrastructure.adapter;

import br.com.horizon.horizon_api.domain.model.UserProfile;
import br.com.horizon.horizon_api.domain.port.UserProfilePersistencePort;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.UserProfileEntity;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.UserEntity;
import br.com.horizon.horizon_api.infrastructure.persistence.repository.UserProfileRepository;
import br.com.horizon.horizon_api.infrastructure.persistence.repository.UserRepository;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserProfilePersistenceAdapter implements UserProfilePersistencePort {
    
    private final UserProfileRepository userProfileRepo;
    private final UserRepository userRepo;

    @Override
    public Optional<UserProfile> findByUserId(Long userId) {
        return userProfileRepo.findByUser_Id(userId).map(e -> {
            UserProfile p = new UserProfile();
            p.setId(e.getId());
            p.setUserId(e.getUser().getId());
            p.setBudgetPerPerson(e.getBudgetPerPerson());
            p.setCreatedAt(e.getCreatedAt());
            p.setUpdatedAt(e.getUpdatedAt());
            return p;
        });
    }

    @Override
    public UserProfile save(UserProfile profile) {
        UserProfileEntity e;
        if (profile.getId() != null) {
            e = userProfileRepo.findById(profile.getId()).orElse(new UserProfileEntity());
        } else {
            e = userProfileRepo.findByUser_Id(profile.getUserId()).orElse(new UserProfileEntity());
        }
        
        UserEntity user = userRepo.findById(profile.getUserId()).orElseThrow();
        e.setUser(user);
        e.setBudgetPerPerson(profile.getBudgetPerPerson());
        e.setCreatedAt(profile.getCreatedAt());
        e.setUpdatedAt(profile.getUpdatedAt());
        
        e = userProfileRepo.save(e);
        
        profile.setId(e.getId());
        return profile;
    }
}
