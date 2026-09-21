package br.com.horizon.horizon_api.domain.port;
import br.com.horizon.horizon_api.domain.model.*;
public interface BehaviorProfileAttributePersistencePort {
    java.util.Optional<BehaviorProfileAttribute> findByUserIdAndAttributeId(Long userId, Long attributeId); void save(BehaviorProfileAttribute bpa); java.util.List<BehaviorProfileAttribute> getBehaviorProfilesByUserId(Long userId);
}
