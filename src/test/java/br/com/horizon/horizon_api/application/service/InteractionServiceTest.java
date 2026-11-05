package br.com.horizon.horizon_api.application.service;

import br.com.horizon.horizon_api.domain.model.*;
import br.com.horizon.horizon_api.domain.port.*;
import br.com.horizon.horizon_api.application.dto.request.InteractionRequest;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.InteractionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.math.BigDecimal;
import java.util.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class InteractionServiceTest {
    @Mock private InteractionPersistencePort interactionPort;
    @Mock private PostPersistencePort postPort;
    @Mock private PostAttributePersistencePort postAttrPort;
    @Mock private BehaviorProfileAttributePersistencePort behaviorPort;
    @Mock private UserPersistencePort userPort;
    @Mock private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    
    @InjectMocks private InteractionService service;
    
    @BeforeEach void setUp() { 
        MockitoAnnotations.openMocks(this);
        when(userPort.findById(1L)).thenReturn(Optional.of(new User()));
        when(postPort.findById(10L)).thenReturn(Optional.of(new Post()));
    }
    
    @Test void shouldRegisterLikeAndIncreaseScore() {
        InteractionRequest req = new InteractionRequest();
        req.setUserId(1L); req.setPostId(10L); req.setInteractionType(InteractionType.LIKE);
        
        when(interactionPort.findByUserIdAndPostId(1L, 10L)).thenReturn(Optional.empty());
        
        PostAttribute pa = new PostAttribute(); pa.setAttributeId(5L); pa.setWeight(new BigDecimal("1.0"));
        when(postAttrPort.findByPostId(10L)).thenReturn(List.of(pa));
        
        Interaction inter = new Interaction();
        inter.setUserId(1L); inter.setPostId(10L); inter.setInteractionType(InteractionType.LIKE);
        when(interactionPort.getByUserId(1L)).thenReturn(List.of(inter));
        
        service.execute(req);
        
        verify(interactionPort).save(any(Interaction.class));
        verify(jdbcTemplate, atLeastOnce()).update(
            contains("INSERT INTO behavior_profile_attributes"),
            eq(1L),
            eq(5L),
            argThat((BigDecimal score) -> score != null && score.compareTo(new BigDecimal("0.1")) == 0),
            any()
        );
    }
    
    @Test void shouldRegisterDislikeAndDecreaseScore() {
        InteractionRequest req = new InteractionRequest();
        req.setUserId(1L); req.setPostId(10L); req.setInteractionType(InteractionType.DISLIKE);
        
        when(interactionPort.findByUserIdAndPostId(1L, 10L)).thenReturn(Optional.empty());
        
        PostAttribute pa = new PostAttribute(); pa.setAttributeId(5L); pa.setWeight(new BigDecimal("1.0"));
        when(postAttrPort.findByPostId(10L)).thenReturn(List.of(pa));
        
        Interaction inter = new Interaction();
        inter.setUserId(1L); inter.setPostId(10L); inter.setInteractionType(InteractionType.DISLIKE);
        when(interactionPort.getByUserId(1L)).thenReturn(List.of(inter));
        
        service.execute(req);
        
        verify(interactionPort).save(any(Interaction.class));
        verify(jdbcTemplate, atLeastOnce()).update(eq("DELETE FROM behavior_profile_attributes WHERE user_id = ?"), eq(1L));
    }
    
    @Test void shouldClampScoreBetweenZeroAndOne() {
        InteractionRequest req = new InteractionRequest();
        req.setUserId(1L); req.setPostId(10L); req.setInteractionType(InteractionType.LIKE);
        
        when(interactionPort.findByUserIdAndPostId(1L, 10L)).thenReturn(Optional.empty());
        
        PostAttribute pa = new PostAttribute(); pa.setAttributeId(5L); pa.setWeight(new BigDecimal("1.0"));
        when(postAttrPort.findByPostId(10L)).thenReturn(List.of(pa));
        
        List<Interaction> interactions = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            Interaction inter = new Interaction();
            inter.setUserId(1L); inter.setPostId(10L); inter.setInteractionType(InteractionType.LIKE);
            interactions.add(inter);
        }
        when(interactionPort.getByUserId(1L)).thenReturn(interactions);
        
        service.execute(req);
        
        verify(interactionPort).save(any(Interaction.class));
        verify(jdbcTemplate, atLeastOnce()).update(contains("INSERT INTO behavior_profile_attributes"), eq(1L), eq(5L), eq(BigDecimal.ONE), any());
    }
}
