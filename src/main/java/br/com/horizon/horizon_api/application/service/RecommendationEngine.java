package br.com.horizon.horizon_api.application.service;
import br.com.horizon.horizon_api.domain.model.*;
import br.com.horizon.horizon_api.domain.port.*;
import br.com.horizon.horizon_api.application.dto.response.RecommendationResponseDTO;
import br.com.horizon.horizon_api.application.usecase.GetRecommendationsUseCase;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationEngine implements GetRecommendationsUseCase {
    private final UserProfileAttributePersistencePort explicitPort;
    private final UserProfilePersistencePort userProfilePort;
    private final BehaviorProfileAttributePersistencePort behaviorPort;
    private final DestinationPersistencePort destinationPort;
    
    @Override
    public List<RecommendationResponseDTO> execute(Long userId, Integer limit) {
        if (limit == null || limit <= 0 || limit > 100) limit = 10;
        
        List<UserProfileAttribute> explicit = explicitPort.getUserProfilesByUserId(userId);
        List<BehaviorProfileAttribute> behavior = behaviorPort.getBehaviorProfilesByUserId(userId);
        
        Map<String, BigDecimal> explicitMap = new HashMap<>();
        UserProfile profile = userProfilePort.findByUserId(userId).orElse(null);
        BigDecimal budget = profile != null ? profile.getBudgetPerPerson() : null;
        for (UserProfileAttribute e : explicit) {
            if (e.getAttributeName() != null) {
                explicitMap.put(e.getAttributeName().toLowerCase(), e.getScore());
            }
        }
        
        Map<String, BigDecimal> behaviorMap = new HashMap<>();
        for (BehaviorProfileAttribute b : behavior) {
            if (b.getAttributeName() != null) {
                behaviorMap.put(b.getAttributeName().toLowerCase(), b.getScore());
            }
        }
        
        List<Destination> destinations = destinationPort.findActive();
        List<RecommendationResult> results = new ArrayList<>();
        
        for (Destination dest : destinations) {
            BigDecimal explicitScore = calculateCompatibility(dest.getAttributes(), explicitMap);
            BigDecimal behaviorScore = calculateCompatibility(dest.getAttributes(), behaviorMap);
            
            boolean coldExplicit = explicitMap.isEmpty();
            boolean coldBehavior = behaviorMap.isEmpty();
            
            if (coldExplicit) explicitScore = new BigDecimal("0.5");
            if (coldBehavior) behaviorScore = new BigDecimal("0.5");
            
            BigDecimal baseScore = explicitScore.multiply(new BigDecimal("0.6")).add(behaviorScore.multiply(new BigDecimal("0.4")));
            
            BigDecimal budgetFactor = BigDecimal.ONE;
            if (budget != null && dest.getBasePrice() != null) {
                if (dest.getBasePrice().compareTo(budget) > 0) {
                    BigDecimal excess = dest.getBasePrice().subtract(budget);
                    BigDecimal ratio = excess.divide(budget, 4, RoundingMode.HALF_UP);
                    budgetFactor = BigDecimal.ONE.subtract(ratio);
                    if (budgetFactor.compareTo(BigDecimal.ZERO) < 0) {
                        budgetFactor = BigDecimal.ZERO;
                    }
                }
            }
            
            BigDecimal finalScore = baseScore.multiply(budgetFactor).setScale(4, RoundingMode.HALF_UP);
            if (finalScore.compareTo(BigDecimal.ONE) > 0) finalScore = BigDecimal.ONE;
            if (finalScore.compareTo(BigDecimal.ZERO) < 0) finalScore = BigDecimal.ZERO;
            
            RecommendationResult r = new RecommendationResult();
            r.destination = dest;
            r.explicitScore = explicitScore;
            r.finalScore = finalScore;
            r.explanation = generateExplanation(dest, explicitMap, behaviorMap, budgetFactor, budget, coldExplicit && coldBehavior);
            results.add(r);
        }
        
        final BigDecimal finalBudget = budget;
        results.sort((r1, r2) -> {
            int cmp = r2.finalScore.compareTo(r1.finalScore);
            if (cmp != 0) return cmp;
            cmp = r2.explicitScore.compareTo(r1.explicitScore);
            if (cmp != 0) return cmp;
            
            BigDecimal price1 = r1.destination.getBasePrice() != null ? r1.destination.getBasePrice() : BigDecimal.ZERO;
            BigDecimal price2 = r2.destination.getBasePrice() != null ? r2.destination.getBasePrice() : BigDecimal.ZERO;
            BigDecimal diff1 = finalBudget != null ? price1.subtract(finalBudget).abs() : BigDecimal.ZERO;
            BigDecimal diff2 = finalBudget != null ? price2.subtract(finalBudget).abs() : BigDecimal.ZERO;
            
            cmp = diff1.compareTo(diff2);
            if (cmp != 0) return cmp;
            
            return r1.destination.getId().compareTo(r2.destination.getId());
        });
        
        return results.stream().limit(limit).map(r -> {
            RecommendationResponseDTO dto = new RecommendationResponseDTO();
            dto.setDestinationId(r.destination.getId());
            dto.setDestinationName(r.destination.getName());
            dto.setCountry(r.destination.getCountry());
            dto.setCity(r.destination.getCity());
            dto.setTourismType(r.destination.getTourismType());
            dto.setImageUrl(r.destination.getImageUrl());
            dto.setBasePrice(r.destination.getBasePrice());
            dto.setScore(r.finalScore);
            dto.setExplanation(r.explanation);
            return dto;
        }).collect(Collectors.toList());
    }
    
    private BigDecimal calculateCompatibility(List<DestinationAttribute> destAttrs, Map<String, BigDecimal> userMap) {
        if (destAttrs == null || destAttrs.isEmpty()) return BigDecimal.ZERO;
        BigDecimal sum = BigDecimal.ZERO;
        for (DestinationAttribute da : destAttrs) {
            String attrName = da.getAttributeName() != null ? da.getAttributeName().toLowerCase() : "";
            BigDecimal userScore = userMap.getOrDefault(attrName, BigDecimal.ZERO);
            BigDecimal diff = userScore.subtract(da.getScore() != null ? da.getScore() : BigDecimal.ZERO).abs();
            BigDecimal match = BigDecimal.ONE.subtract(diff);
            if (match.compareTo(BigDecimal.ZERO) < 0) match = BigDecimal.ZERO;
            sum = sum.add(match);
        }
        return sum.divide(new BigDecimal(destAttrs.size()), 4, RoundingMode.HALF_UP);
    }
    
    private String generateExplanation(Destination dest, Map<String, BigDecimal> explicitMap, Map<String, BigDecimal> behaviorMap, BigDecimal budgetFactor, BigDecimal budget, boolean isColdStart) {
        if (isColdStart) {
            return "Recomendado como um destino popular e excelente ponto de partida.";
        }
        
        List<String> explicitTopAttrs = new ArrayList<>();
        List<String> behaviorTopAttrs = new ArrayList<>();
        if (dest.getAttributes() != null) {
            for (DestinationAttribute da : dest.getAttributes()) {
                String name = da.getAttributeName() != null ? da.getAttributeName().toLowerCase() : "";
                BigDecimal expScore = explicitMap.getOrDefault(name, BigDecimal.ZERO);
                BigDecimal behScore = behaviorMap.getOrDefault(name, BigDecimal.ZERO);
                BigDecimal destScore = da.getScore() != null ? da.getScore() : BigDecimal.ZERO;
                
                if (destScore.compareTo(new BigDecimal("0.5")) >= 0) {
                    if (expScore.compareTo(new BigDecimal("0.5")) >= 0) {
                        explicitTopAttrs.add(name);
                    }
                    if (behScore.compareTo(new BigDecimal("0.5")) >= 0 && expScore.compareTo(new BigDecimal("0.5")) < 0) {
                        behaviorTopAttrs.add(name);
                    }
                }
            }
        }
        
        String exp = "";
        if (!explicitTopAttrs.isEmpty()) {
            String attrStr = String.join(" e ", explicitTopAttrs.subList(0, Math.min(explicitTopAttrs.size(), 2)));
            exp = "Alta compatibilidade com suas preferências de " + attrStr;
        }
        
        if (!behaviorTopAttrs.isEmpty()) {
            String behStr = String.join(" e ", behaviorTopAttrs.subList(0, Math.min(behaviorTopAttrs.size(), 2)));
            if (exp.isEmpty()) {
                exp = "Recomendado por suas interações com conteúdos de " + behStr;
            } else {
                exp += ", reforçada por suas interações com conteúdos de " + behStr;
            }
        }
        
        if (exp.isEmpty()) {
            exp = "Recomendado com base no seu perfil de viajante";
        }
        exp += ".";
        
        if (budget != null) {
            if (budgetFactor.compareTo(BigDecimal.ONE) == 0) {
                // exp += " E por estar dentro do seu orçamento."; // keep it cleaner without budget mentions if 100% matched
            } else if (budgetFactor.compareTo(new BigDecimal("0.8")) >= 0) {
                exp += " Apesar de estar um pouco acima do orçamento.";
            } else {
                exp += " Porém, encontra-se significativamente acima do seu orçamento.";
            }
        }
        return exp;
    }
    
    private static class RecommendationResult {
        Destination destination;
        BigDecimal explicitScore;
        BigDecimal finalScore;
        String explanation;
    }
}
