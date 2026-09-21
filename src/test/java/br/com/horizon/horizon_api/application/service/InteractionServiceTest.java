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
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class InteractionServiceTest {
    @Mock private InteractionPersistencePort interactionPort;
    @Mock private PostPersistencePort postPort;
    @Mock private PostAttributePersistencePort postAttrPort;
    @Mock private BehaviorProfileAttributePersistencePort behaviorPort;
    @Mock private UserPersistencePort userPort;
    
    @InjectMocks private InteractionService service;
    
    @BeforeEach void setUp() { MockitoAnnotations.openMocks(this); }
    
    @Test void shouldRegisterLikeAndIncreaseScore() {
        InteractionRequest req = new InteractionRequest();
        req.setUserId(1L); req.setPostId(10L); req.setInteractionType(InteractionType.LIKE);
        
        when(userPort.findById(1L)).thenReturn(Optional.of(new User()));
        when(postPort.findById(10L)).thenReturn(Optional.of(new Post()));
        when(interactionPort.findByUserIdAndPostId(1L, 10L)).thenReturn(Optional.empty()); // No prev interaction
        
        PostAttribute pa = new PostAttribute(); pa.setAttributeId(5L); pa.setWeight(new BigDecimal("1.0"));
        when(postAttrPort.findByPostId(10L)).thenReturn(List.of(pa));
        
        BehaviorProfileAttribute bpa = new BehaviorProfileAttribute(); bpa.setUserId(1L); bpa.setAttributeId(5L); bpa.setScore(new BigDecimal("0.5"));
        when(behaviorPort.findByUserIdAndAttributeId(1L, 5L)).thenReturn(Optional.of(bpa));
        
        service.execute(req);
        
        verify(behaviorPort).save(argThat(b -> b.getScore().compareTo(new BigDecimal("0.6")) == 0));
        verify(interactionPort).save(any(Interaction.class));
    }
    
    @Test void shouldRegisterDislikeAndDecreaseScore() {
        InteractionRequest req = new InteractionRequest();
        req.setUserId(1L); req.setPostId(10L); req.setInteractionType(InteractionType.DISLIKE);
        
        when(userPort.findById(1L)).thenReturn(Optional.of(new User()));
        when(postPort.findById(10L)).thenReturn(Optional.of(new Post()));
        when(interactionPort.findByUserIdAndPostId(1L, 10L)).thenReturn(Optional.empty());
        
        PostAttribute pa = new PostAttribute(); pa.setAttributeId(5L); pa.setWeight(new BigDecimal("1.0"));
        when(postAttrPort.findByPostId(10L)).thenReturn(List.of(pa));
        
        BehaviorProfileAttribute bpa = new BehaviorProfileAttribute(); bpa.setUserId(1L); bpa.setAttributeId(5L); bpa.setScore(new BigDecimal("0.1"));
        when(behaviorPort.findByUserIdAndAttributeId(1L, 5L)).thenReturn(Optional.of(bpa));
        
        service.execute(req);
        
        verify(behaviorPort).save(argThat(b -> b.getScore().compareTo(new BigDecimal("0.0")) == 0)); // 0.1 - 0.1 = 0.0
    }
    
    @Test void shouldClampScoreBetweenZeroAndOne() {
        InteractionRequest req = new InteractionRequest();
        req.setUserId(1L); req.setPostId(10L); req.setInteractionType(InteractionType.LIKE);
        
        when(userPort.findById(1L)).thenReturn(Optional.of(new User()));
        when(postPort.findById(10L)).thenReturn(Optional.of(new Post()));
        when(interactionPort.findByUserIdAndPostId(1L, 10L)).thenReturn(Optional.empty());
        
        PostAttribute pa = new PostAttribute(); pa.setAttributeId(5L); pa.setWeight(new BigDecimal("1.0"));
        when(postAttrPort.findByPostId(10L)).thenReturn(List.of(pa));
        
        BehaviorProfileAttribute bpa = new BehaviorProfileAttribute(); bpa.setUserId(1L); bpa.setAttributeId(5L); bpa.setScore(new BigDecimal("0.95"));
        when(behaviorPort.findByUserIdAndAttributeId(1L, 5L)).thenReturn(Optional.of(bpa));
        
        service.execute(req);
        
        // 0.95 + 0.1 = 1.05 -> clamped to 1.0
        verify(behaviorPort).save(argThat(b -> b.getScore().compareTo(BigDecimal.ONE) == 0));
    }
}
