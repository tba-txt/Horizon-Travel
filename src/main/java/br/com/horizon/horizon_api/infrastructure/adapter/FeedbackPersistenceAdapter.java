package br.com.horizon.horizon_api.infrastructure.adapter;

import br.com.horizon.horizon_api.domain.model.Feedback;
import br.com.horizon.horizon_api.domain.port.FeedbackPersistencePort;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.FeedbackEntity;
import br.com.horizon.horizon_api.infrastructure.persistence.repository.FeedbackRepository;
import br.com.horizon.horizon_api.application.mapper.FeedbackMapper;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class FeedbackPersistenceAdapter implements FeedbackPersistencePort {

    private final FeedbackRepository repository;
    private final FeedbackMapper mapper;
    private final br.com.horizon.horizon_api.infrastructure.persistence.repository.UserRepository userRepository;
    private final br.com.horizon.horizon_api.infrastructure.persistence.repository.ReservationRepository reservationRepository;

    @Override
    public Feedback save(Feedback domain) {
        FeedbackEntity entity = mapper.toEntity(domain);
        if (domain.getUserId() != null) {
            entity.setUser(userRepository.findById(domain.getUserId()).orElse(null));
        }
        if (domain.getReservationId() != null) {
            entity.setReservation(reservationRepository.findById(domain.getReservationId()).orElse(null));
        }
        FeedbackEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Feedback> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Feedback> findAll() {
        return repository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }

    @Override
    public boolean existsByUserIdAndReservationIdAndFeedbackTypeAndTargetType(Long userId, Long reservationId, br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.FeedbackType feedbackType, br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.TargetType targetType) {
        return repository.existsByUserIdAndReservation_IdAndFeedbackTypeAndTargetType(
                userId, reservationId, 
                feedbackType, 
                targetType);
    }

    @Override
    public boolean existsByUserIdAndFeedbackTypeAndTargetType(Long userId, br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.FeedbackType feedbackType, br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.TargetType targetType) {
        return repository.existsByUserIdAndFeedbackTypeAndTargetType(
                userId, 
                feedbackType, 
                targetType);
    }
}
