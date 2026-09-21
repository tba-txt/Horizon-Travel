package br.com.horizon.horizon_api.infrastructure.adapter;
import br.com.horizon.horizon_api.domain.model.Destination;
import br.com.horizon.horizon_api.domain.model.DestinationAttribute;
import br.com.horizon.horizon_api.domain.port.DestinationPersistencePort;
import br.com.horizon.horizon_api.infrastructure.persistence.repository.DestinationRepository;
import br.com.horizon.horizon_api.infrastructure.persistence.repository.DestinationAttributeRepository;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.DestinationEntity;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.DestinationAttributeEntity;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DestinationPersistenceAdapter implements DestinationPersistencePort {
    private final DestinationRepository destRepo;
    private final DestinationAttributeRepository attrRepo;
    
    @Override
    public List<Destination> findActive() {
        List<DestinationEntity> entities = destRepo.findByActiveTrue();
        if (entities.isEmpty()) return new ArrayList<>();
        
        List<Long> destIds = entities.stream().map(DestinationEntity::getId).collect(Collectors.toList());
        List<DestinationAttributeEntity> allAttrs = attrRepo.findByDestination_IdIn(destIds);
        
        Map<Long, List<DestinationAttribute>> attrsByDest = allAttrs.stream().map(a -> {
            DestinationAttribute da = new DestinationAttribute();
            da.setAttributeId(a.getAttribute().getId());
            if (a.getAttribute() != null) da.setAttributeName(a.getAttribute().getName());
            da.setScore(a.getScore());
            return new Object[]{a.getDestination().getId(), da};
        }).collect(Collectors.groupingBy(
            arr -> (Long) arr[0],
            Collectors.mapping(arr -> (DestinationAttribute) arr[1], Collectors.toList())
        ));
        
        return entities.stream().map(e -> {
            Destination d = new Destination();
            d.setId(e.getId());
            d.setName(e.getName());
            d.setCountry(e.getCountry());
            d.setCity(e.getCity());
            if (e.getTourismType() != null) d.setTourismType(e.getTourismType().name());
            d.setImageUrl(e.getImageUrl());
            d.setBasePrice(e.getBasePrice());
            d.setLatitude(e.getLatitude());
            d.setLongitude(e.getLongitude());
            d.setActive(e.getActive());
            d.setCreatedAt(e.getCreatedAt());
            d.setAttributes(attrsByDest.getOrDefault(e.getId(), new ArrayList<>()));
            return d;
        }).collect(Collectors.toList());
    }

    @Override
    public Optional<Destination> findById(Long id) {
        return destRepo.findById(id).map(e -> {
            Destination d = new Destination();
            d.setId(e.getId());
            d.setName(e.getName());
            d.setCountry(e.getCountry());
            d.setCity(e.getCity());
            if (e.getTourismType() != null) d.setTourismType(e.getTourismType().name());
            d.setImageUrl(e.getImageUrl());
            d.setBasePrice(e.getBasePrice());
            d.setLatitude(e.getLatitude());
            d.setLongitude(e.getLongitude());
            d.setActive(e.getActive());
            d.setCreatedAt(e.getCreatedAt());
            return d;
        });
    }
}
