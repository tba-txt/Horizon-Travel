package br.com.horizon.horizon_api.infrastructure.adapter;
import br.com.horizon.horizon_api.domain.model.UserProfileAttribute;
import br.com.horizon.horizon_api.domain.model.BehaviorProfileAttribute;
import br.com.horizon.horizon_api.domain.port.UserProfileAttributePersistencePort;
import br.com.horizon.horizon_api.domain.port.BehaviorProfileAttributePersistencePort;
import br.com.horizon.horizon_api.infrastructure.persistence.repository.UserProfileAttributeRepository;
import br.com.horizon.horizon_api.infrastructure.persistence.repository.BehaviorProfileAttributeRepository;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.UserProfileAttributeEntity;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.BehaviorProfileAttributeEntity;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.UserEntity;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.AttributeEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProfilePersistenceAdapter implements UserProfileAttributePersistencePort, BehaviorProfileAttributePersistencePort {
    private final UserProfileAttributeRepository userRepo;
    private final BehaviorProfileAttributeRepository behaviorRepo;
    
    @Override
    @Transactional
    public void deleteByUserId(Long userId) {
        userRepo.deleteByUser_Id(userId);
        userRepo.flush();
    }
    
    @Override
    public void saveAll(List<UserProfileAttribute> attributes) {
        List<UserProfileAttributeEntity> entities = attributes.stream().map(a -> {
            UserProfileAttributeEntity e = new UserProfileAttributeEntity();
            e.setId(a.getId());
            UserEntity u = new UserEntity(); u.setId(a.getUserId()); e.setUser(u);
            AttributeEntity attr = new AttributeEntity(); attr.setId(a.getAttributeId()); e.setAttribute(attr);
            e.setScore(a.getScore());
            e.setUpdatedAt(a.getUpdatedAt());
            return e;
        }).collect(Collectors.toList());
        userRepo.saveAll(entities);
    }
    
    @Override
    public List<UserProfileAttribute> getUserProfilesByUserId(Long userId) {
        return userRepo.findByUser_Id(userId).stream().map(e -> {
            UserProfileAttribute a = new UserProfileAttribute();
            a.setId(e.getId());
            a.setUserId(e.getUser().getId());
            a.setAttributeId(e.getAttribute().getId());
            if (e.getAttribute() != null) a.setAttributeName(e.getAttribute().getName());
            a.setScore(e.getScore());
            a.setUpdatedAt(e.getUpdatedAt());
            return a;
        }).collect(Collectors.toList());
    }
    
    @Override
    public Optional<BehaviorProfileAttribute> findByUserIdAndAttributeId(Long userId, Long attributeId) {
        return behaviorRepo.findByUser_IdAndAttribute_Id(userId, attributeId).map(e -> {
            BehaviorProfileAttribute a = new BehaviorProfileAttribute();
            a.setId(e.getId());
            a.setUserId(e.getUser().getId());
            a.setAttributeId(e.getAttribute().getId());
            if (e.getAttribute() != null) a.setAttributeName(e.getAttribute().getName());
            a.setScore(e.getScore());
            a.setUpdatedAt(e.getUpdatedAt());
            return a;
        });
    }
    
    @Override
    public void save(BehaviorProfileAttribute bpa) {
        BehaviorProfileAttributeEntity e = new BehaviorProfileAttributeEntity();
        e.setId(bpa.getId());
        UserEntity u = new UserEntity(); u.setId(bpa.getUserId()); e.setUser(u);
        AttributeEntity attr = new AttributeEntity(); attr.setId(bpa.getAttributeId()); e.setAttribute(attr);
        e.setScore(bpa.getScore());
        e.setUpdatedAt(bpa.getUpdatedAt());
        behaviorRepo.save(e);
    }
    
    @Override
    public List<BehaviorProfileAttribute> getBehaviorProfilesByUserId(Long userId) {
        return behaviorRepo.findByUser_Id(userId).stream().map(e -> {
            BehaviorProfileAttribute a = new BehaviorProfileAttribute();
            a.setId(e.getId());
            a.setUserId(e.getUser().getId());
            a.setAttributeId(e.getAttribute().getId());
            if (e.getAttribute() != null) a.setAttributeName(e.getAttribute().getName());
            a.setScore(e.getScore());
            a.setUpdatedAt(e.getUpdatedAt());
            return a;
        }).collect(Collectors.toList());
    }
}
