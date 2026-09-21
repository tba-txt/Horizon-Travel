package br.com.horizon.horizon_api.infrastructure.persistence.repository;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.DestinationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface DestinationRepository extends JpaRepository<DestinationEntity, Long> {
    List<DestinationEntity> findByActiveTrue();
}
