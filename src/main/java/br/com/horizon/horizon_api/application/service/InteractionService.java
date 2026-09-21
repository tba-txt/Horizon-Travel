package br.com.horizon.horizon_api.application.service;
import br.com.horizon.horizon_api.domain.model.*;
import br.com.horizon.horizon_api.domain.port.*;
import br.com.horizon.horizon_api.domain.exception.ResourceNotFoundException;
import br.com.horizon.horizon_api.application.dto.request.InteractionRequest;
import br.com.horizon.horizon_api.application.usecase.RegisterInteractionUseCase;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.InteractionType;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.time.OffsetDateTime;
import java.util.*;
import java.math.BigDecimal;

import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class InteractionService implements RegisterInteractionUseCase {
    private final InteractionPersistencePort interactionPort;
    private final PostPersistencePort postPort;
    private final PostAttributePersistencePort postAttrPort;
    private final BehaviorProfileAttributePersistencePort behaviorPort;
    private final UserPersistencePort userPort;
    
    @Override
    public void execute(InteractionRequest req) {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Long)) {
            throw new ResourceNotFoundException("Usuário não autenticado.");
        }
        Long userId = (Long) auth.getPrincipal();

        userPort.findById(userId).orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
        postPort.findById(req.getPostId()).orElseThrow(() -> new ResourceNotFoundException("Post não encontrado."));
        
        Optional<Interaction> prevOpt = interactionPort.findByUserIdAndPostId(userId, req.getPostId());
        InteractionType oldType = prevOpt.map(Interaction::getInteractionType).orElse(null);
        InteractionType newType = req.getInteractionType();
        
        boolean isRemoving = (oldType == newType);
        
        List<PostAttribute> attrs = postAttrPort.findByPostId(req.getPostId());
        OffsetDateTime now = OffsetDateTime.now();
        
        for (PostAttribute pa : attrs) {
            BehaviorProfileAttribute bpa = behaviorPort.findByUserIdAndAttributeId(userId, pa.getAttributeId())
                .orElseGet(() -> {
                    BehaviorProfileAttribute a = new BehaviorProfileAttribute();
                    a.setUserId(userId);
                    a.setAttributeId(pa.getAttributeId());
                    a.setScore(BigDecimal.ZERO);
                    return a;
                });
                
            BigDecimal delta = BigDecimal.ZERO;
            BigDecimal weight = pa.getWeight() == null ? BigDecimal.ONE : pa.getWeight();
            BigDecimal step = new BigDecimal("0.1");
            
            if (oldType == InteractionType.LIKE) delta = delta.subtract(weight.multiply(step));
            if (oldType == InteractionType.DISLIKE) delta = delta.add(weight.multiply(step));
            
            if (!isRemoving) {
                if (newType == InteractionType.LIKE) delta = delta.add(weight.multiply(step));
                if (newType == InteractionType.DISLIKE) delta = delta.subtract(weight.multiply(step));
            }
            
            BigDecimal newScore = bpa.getScore().add(delta);
            if (newScore.compareTo(BigDecimal.ONE) > 0) newScore = BigDecimal.ONE;
            if (newScore.compareTo(BigDecimal.ZERO) < 0) newScore = BigDecimal.ZERO;
            
            bpa.setScore(newScore);
            bpa.setUpdatedAt(now);
            behaviorPort.save(bpa);
        }
        
        if (isRemoving) {
            interactionPort.delete(prevOpt.get().getId());
        } else {
            Interaction i = prevOpt.orElse(new Interaction());
            i.setUserId(userId);
            i.setPostId(req.getPostId());
            i.setInteractionType(newType);
            if (i.getId() == null) i.setCreatedAt(now);
            interactionPort.save(i);
        }
    }
}
