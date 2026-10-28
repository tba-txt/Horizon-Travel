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
import java.util.stream.Collectors;
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
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    
    @Override
    public void execute(InteractionRequest req) {
        final Long userId;
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Long) {
            userId = (Long) auth.getPrincipal();
        } else {
            userId = req.getUserId();
        }

        if (userId == null) {
            throw new ResourceNotFoundException("Usuário não autenticado.");
        }

        userPort.findById(userId).orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
        postPort.findById(req.getPostId()).orElseThrow(() -> new ResourceNotFoundException("Post não encontrado."));
        
        // Lock exclusivo na linha do usuário para serializar requisições concorrentes do mesmo usuário
        jdbcTemplate.queryForObject("SELECT id FROM users WHERE id = ? FOR UPDATE", Long.class, userId);
        
        Long resolvedOptionId = resolveOptionId(req.getPostId(), req.getOptionId());
        req.setOptionId(resolvedOptionId);

        Optional<Interaction> prevOpt = interactionPort.findByUserIdAndPostId(userId, req.getPostId());
        InteractionType oldType = prevOpt.map(Interaction::getInteractionType).orElse(null);
        InteractionType newType = req.getInteractionType();
        Long oldOptionId = prevOpt.map(i -> resolveOptionId(i.getPostId(), i.getOptionId())).orElse(null);
        
        boolean isRemoving = (oldType == newType) && 
                             (newType != InteractionType.VOTE || Objects.equals(oldOptionId, resolvedOptionId));
        
        OffsetDateTime now = OffsetDateTime.now();
        
        // 1. Save or Delete Interaction
        if (isRemoving) {
            interactionPort.delete(prevOpt.get().getId());
        } else {
            Interaction i = prevOpt.orElse(new Interaction());
            i.setUserId(userId);
            i.setPostId(req.getPostId());
            i.setInteractionType(newType);
            i.setOptionId(resolvedOptionId);
            if (i.getId() == null) i.setCreatedAt(now);
            interactionPort.save(i);
        }

        // 2. Recalcular o perfil comportamental completo a partir das interações reais do usuário (LIKE, DISLIKE, VOTE)
        recalculateBehaviorProfile(userId, now);

        // 3. Sincronizar contadores físicos nas tabelas posts e post_options
        jdbcTemplate.update(
            "UPDATE posts SET " +
            "likes_count = (SELECT count(*) FROM interactions WHERE post_id = ? AND interaction_type = 'LIKE'), " +
            "dislikes_count = (SELECT count(*) FROM interactions WHERE post_id = ? AND interaction_type = 'DISLIKE') " +
            "WHERE id = ?",
            req.getPostId(), req.getPostId(), req.getPostId()
        );
        
        if (resolvedOptionId != null || oldType == InteractionType.VOTE) {
            jdbcTemplate.update(
                "UPDATE post_options SET votes_count = (SELECT count(*) FROM interactions WHERE option_id = post_options.id AND interaction_type = 'VOTE') WHERE post_id = ?",
                req.getPostId()
            );
        }
    }

    private Long resolveOptionId(Long postId, Long optionId) {
        if (optionId == null || postId == null) return null;
        List<Long> postOptionIds = jdbcTemplate.query(
            "SELECT id FROM post_options WHERE post_id = ? ORDER BY id ASC",
            (rs, rowNum) -> rs.getLong("id"),
            postId
        );
        if (postOptionIds.contains(optionId)) {
            return optionId;
        }
        if (optionId == 1L && postOptionIds.size() >= 1) {
            return postOptionIds.get(0);
        }
        if (optionId == 2L && postOptionIds.size() >= 2) {
            return postOptionIds.get(1);
        }
        return optionId;
    }

    private void recalculateBehaviorProfile(Long userId, OffsetDateTime now) {
        List<Interaction> allInteractions = interactionPort.getByUserId(userId);
        Map<Long, BigDecimal> rawScores = new TreeMap<>();
        BigDecimal step = new BigDecimal("0.1");

        for (Interaction inter : allInteractions) {
            InteractionType type = inter.getInteractionType();
            if (type == null) continue;
            Long optId = (type == InteractionType.VOTE) ? resolveOptionId(inter.getPostId(), inter.getOptionId()) : null;
            List<PostAttribute> attrs = getAttributesForInteraction(inter.getPostId(), optId, type);

            for (PostAttribute pa : attrs) {
                if (pa.getAttributeId() == null) continue;
                BigDecimal w = pa.getWeight() != null ? pa.getWeight() : BigDecimal.ONE;
                BigDecimal delta = w.multiply(step);
                BigDecimal current = rawScores.getOrDefault(pa.getAttributeId(), BigDecimal.ZERO);

                if (type == InteractionType.LIKE || type == InteractionType.VOTE) {
                    rawScores.put(pa.getAttributeId(), current.add(delta));
                } else if (type == InteractionType.DISLIKE) {
                    rawScores.put(pa.getAttributeId(), current.subtract(delta));
                }
            }
        }

        // Filtra e aplica clamp (0..1) em ordem crescente determinística
        Map<Long, BigDecimal> finalScores = new TreeMap<>();
        for (Map.Entry<Long, BigDecimal> entry : rawScores.entrySet()) {
            BigDecimal score = entry.getValue();
            if (score.compareTo(BigDecimal.ONE) > 0) score = BigDecimal.ONE;
            if (score.compareTo(BigDecimal.ZERO) < 0) score = BigDecimal.ZERO;

            if (score.compareTo(BigDecimal.ZERO) > 0) {
                finalScores.put(entry.getKey(), score);
            }
        }

        if (finalScores.isEmpty()) {
            jdbcTemplate.update("DELETE FROM behavior_profile_attributes WHERE user_id = ?", userId);
        } else {
            // Remove atributos que não estão mais ativos para este usuário
            String inClause = finalScores.keySet().stream().map(String::valueOf).collect(Collectors.joining(","));
            jdbcTemplate.update("DELETE FROM behavior_profile_attributes WHERE user_id = ? AND attribute_id NOT IN (" + inClause + ")", userId);

            // Upsert atômico determinístico ordenado por attribute_id
            for (Map.Entry<Long, BigDecimal> entry : finalScores.entrySet()) {
                jdbcTemplate.update(
                    "INSERT INTO behavior_profile_attributes (user_id, attribute_id, score, updated_at) " +
                    "VALUES (?, ?, ?, ?) " +
                    "ON CONFLICT (user_id, attribute_id) " +
                    "DO UPDATE SET score = EXCLUDED.score, updated_at = EXCLUDED.updated_at",
                    userId, entry.getKey(), entry.getValue(), now
                );
            }
        }
    }
    
    private List<PostAttribute> getAttributesForInteraction(Long postId, Long optionId, InteractionType type) {
        if (type == InteractionType.VOTE && optionId != null) {
            return jdbcTemplate.query(
                "SELECT attribute_id, weight FROM post_option_attributes WHERE option_id = ?",
                (rs, rowNum) -> {
                    PostAttribute pa = new PostAttribute();
                    pa.setAttributeId(rs.getLong("attribute_id"));
                    pa.setWeight(rs.getBigDecimal("weight"));
                    return pa;
                },
                optionId
            );
        }
        return postAttrPort.findByPostId(postId);
    }
}
