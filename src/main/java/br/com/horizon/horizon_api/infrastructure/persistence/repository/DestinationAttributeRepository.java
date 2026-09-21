package br.com.horizon.horizon_api.infrastructure.persistence.repository;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.DestinationAttributeEntity;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.DestinationAttributeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface DestinationAttributeRepository extends JpaRepository<DestinationAttributeEntity, DestinationAttributeId> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"attribute"})
    List<DestinationAttributeEntity> findByDestination_Id(Long destinationId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"attribute"})
    List<DestinationAttributeEntity> findByDestination_IdIn(List<Long> destinationIds);
}
