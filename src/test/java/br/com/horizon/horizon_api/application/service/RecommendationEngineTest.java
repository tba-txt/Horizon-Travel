package br.com.horizon.horizon_api.application.service;
import br.com.horizon.horizon_api.domain.model.*;
import br.com.horizon.horizon_api.domain.port.*;
import br.com.horizon.horizon_api.application.dto.response.RecommendationResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.math.BigDecimal;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class RecommendationEngineTest {
    @Mock private UserProfileAttributePersistencePort explicitPort;
    @Mock private BehaviorProfileAttributePersistencePort behaviorPort;
    @Mock private DestinationPersistencePort destinationPort;
    @Mock private UserProfilePersistencePort userProfilePort;
    
    @InjectMocks private RecommendationEngine engine;
    
    @BeforeEach void setUp() { MockitoAnnotations.openMocks(this); }
    
    @Test void shouldHandleNullBudget() {
        when(explicitPort.getUserProfilesByUserId(1L)).thenReturn(Collections.emptyList());
        when(behaviorPort.getBehaviorProfilesByUserId(1L)).thenReturn(Collections.emptyList());
        when(userProfilePort.findByUserId(1L)).thenReturn(Optional.empty()); // Null budget
        
        Destination d = new Destination();
        d.setId(10L); d.setBasePrice(new BigDecimal("1000"));
        when(destinationPort.findActive()).thenReturn(List.of(d));
        
        List<RecommendationResponseDTO> res = engine.execute(1L, 10);
        assertEquals(1, res.size());
        assertEquals(new BigDecimal("0.5000"), res.get(0).getScore());
    }
    
    @Test void shouldApplyBudgetFactorCorrectly() {
        UserProfileAttribute u1 = new UserProfileAttribute(); u1.setAttributeName("natureza"); u1.setScore(new BigDecimal("1.0"));
        when(explicitPort.getUserProfilesByUserId(1L)).thenReturn(List.of(u1));
        
        BehaviorProfileAttribute b1 = new BehaviorProfileAttribute(); b1.setAttributeName("aventura"); b1.setScore(new BigDecimal("1.0"));
        when(behaviorPort.getBehaviorProfilesByUserId(1L)).thenReturn(List.of(b1));
        
        UserProfile profile = new UserProfile();
        profile.setBudgetPerPerson(new BigDecimal("2000"));
        when(userProfilePort.findByUserId(1L)).thenReturn(Optional.of(profile));
        
        // Dest 1: Below budget (price 1500)
        Destination d1 = new Destination(); d1.setId(1L); d1.setBasePrice(new BigDecimal("1500"));
        DestinationAttribute da1 = new DestinationAttribute(); da1.setAttributeName("natureza"); da1.setScore(new BigDecimal("1.0"));
        DestinationAttribute da2 = new DestinationAttribute(); da2.setAttributeName("aventura"); da2.setScore(new BigDecimal("1.0"));
        d1.setAttributes(List.of(da1, da2));
        
        // Dest 2: Exact budget (price 2000)
        Destination d2 = new Destination(); d2.setId(2L); d2.setBasePrice(new BigDecimal("2000"));
        DestinationAttribute da3 = new DestinationAttribute(); da3.setAttributeName("natureza"); da3.setScore(new BigDecimal("1.0"));
        DestinationAttribute da4 = new DestinationAttribute(); da4.setAttributeName("aventura"); da4.setScore(new BigDecimal("1.0"));
        d2.setAttributes(List.of(da3, da4));
        
        // Dest 3: Above budget (price 2500) -> Excess 500. Ratio = 500/2000 = 0.25. Factor = 0.75
        Destination d3 = new Destination(); d3.setId(3L); d3.setBasePrice(new BigDecimal("2500"));
        DestinationAttribute da5 = new DestinationAttribute(); da5.setAttributeName("natureza"); da5.setScore(new BigDecimal("1.0"));
        DestinationAttribute da6 = new DestinationAttribute(); da6.setAttributeName("aventura"); da6.setScore(new BigDecimal("1.0"));
        d3.setAttributes(List.of(da5, da6));
        
        
        // Dest 4: 100% Above budget (price 4000) -> Excess 2000. Ratio = 2000/2000 = 1.0. Factor = 0.0
        Destination d4 = new Destination(); d4.setId(4L); d4.setBasePrice(new BigDecimal("4000"));
        DestinationAttribute da7 = new DestinationAttribute(); da7.setAttributeName("natureza"); da7.setScore(new BigDecimal("1.0"));
        DestinationAttribute da8 = new DestinationAttribute(); da8.setAttributeName("aventura"); da8.setScore(new BigDecimal("1.0"));
        d4.setAttributes(List.of(da7, da8));
        
        when(destinationPort.findActive()).thenReturn(List.of(d1, d2, d3, d4));

        
        List<RecommendationResponseDTO> res = engine.execute(1L, 10);
        
        assertEquals(4, res.size());
        // Base score for all is 1.0 (100% match)
        
        // Dest 1 (Below budget) -> factor 1.0 -> final score 1.0000
        Optional<RecommendationResponseDTO> r1 = res.stream().filter(r -> r.getDestinationId() == 1L).findFirst();
        assertEquals(new BigDecimal("1.0000"), r1.get().getScore());
        
        // Dest 2 (Exact budget) -> factor 1.0 -> final score 1.0000
        Optional<RecommendationResponseDTO> r2 = res.stream().filter(r -> r.getDestinationId() == 2L).findFirst();
        assertEquals(new BigDecimal("1.0000"), r2.get().getScore());
        
        // Dest 3 (Above budget) -> factor 0.75 -> final score 0.7500
        Optional<RecommendationResponseDTO> r3 = res.stream().filter(r -> r.getDestinationId() == 3L).findFirst();
        assertEquals(new BigDecimal("0.7500"), r3.get().getScore());
        
        // Dest 4 (100% Above budget) -> factor 0.0 -> final score 0.0000
        Optional<RecommendationResponseDTO> r4 = res.stream().filter(r -> r.getDestinationId() == 4L).findFirst();
        assertEquals(new BigDecimal("0.0000"), r4.get().getScore());
    }
}
