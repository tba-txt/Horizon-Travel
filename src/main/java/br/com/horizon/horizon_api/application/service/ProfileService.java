package br.com.horizon.horizon_api.application.service;
import br.com.horizon.horizon_api.domain.model.*;
import br.com.horizon.horizon_api.domain.port.*;
import br.com.horizon.horizon_api.application.dto.response.ProfileResponseDTO;
import br.com.horizon.horizon_api.application.dto.response.ProfileAttributeDTO;
import br.com.horizon.horizon_api.application.usecase.GetExplicitProfileUseCase;
import br.com.horizon.horizon_api.application.usecase.GetBehaviorProfileUseCase;
import br.com.horizon.horizon_api.application.usecase.ResetAlgorithmUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfileService implements GetExplicitProfileUseCase, GetBehaviorProfileUseCase, ResetAlgorithmUseCase {
    private final UserProfileAttributePersistencePort explicitPort;
    private final BehaviorProfileAttributePersistencePort behaviorPort;
    private final JdbcTemplate jdbcTemplate;
    
    @Override
    public ProfileResponseDTO executeExplicit(Long userId) {
        List<ProfileAttributeDTO> attrs = explicitPort.getUserProfilesByUserId(userId).stream()
            .map(a -> new ProfileAttributeDTO(a.getAttributeId(), a.getScore()))
            .collect(Collectors.toList());
            
        ProfileResponseDTO dto = new ProfileResponseDTO();
        if (attrs.isEmpty()) {
            dto.setHasProfile(false);
            dto.setMessage("Responda ao quiz para criar seu perfil de preferências.");
        } else {
            dto.setHasProfile(true);
            dto.setMessage("Perfil explícito carregado com sucesso.");
            dto.setAttributes(attrs);
        }
        return dto;
    }
    
    @Override
    public ProfileResponseDTO executeBehavior(Long userId) {
        List<ProfileAttributeDTO> attrs = behaviorPort.getBehaviorProfilesByUserId(userId).stream()
            .map(a -> new ProfileAttributeDTO(a.getAttributeId(), a.getScore()))
            .collect(Collectors.toList());
            
        ProfileResponseDTO dto = new ProfileResponseDTO();
        if (attrs.isEmpty()) {
            dto.setHasProfile(false);
            dto.setMessage("Interaja com os conteúdos do feed para construir seu perfil comportamental.");
        } else {
            dto.setHasProfile(true);
            dto.setMessage("Perfil comportamental carregado com sucesso.");
            dto.setAttributes(attrs);
        }
        return dto;
    }

    @Override
    @Transactional
    public void resetAlgorithm(Long userId) {
        if (userId == null) {
            return;
        }

        // Serializa operações concorrentes do mesmo usuário
        jdbcTemplate.queryForObject("SELECT id FROM users WHERE id = ? FOR UPDATE", Long.class, userId);

        // 1. Captura os posts afetados antes de remover as interações (LIKE, DISLIKE, VOTE)
        List<Long> affectedPostIds = jdbcTemplate.queryForList(
            "SELECT DISTINCT post_id FROM interactions WHERE user_id = ?",
            Long.class,
            userId
        );

        // 2. Remove todas as interações do usuário no feed
        jdbcTemplate.update("DELETE FROM interactions WHERE user_id = ?", userId);

        // 3. Sincroniza os contadores físicos nas tabelas posts e post_options para os posts afetados
        for (Long postId : affectedPostIds) {
            if (postId == null) continue;
            jdbcTemplate.update(
                "UPDATE posts SET " +
                "likes_count = (SELECT count(*) FROM interactions WHERE post_id = ? AND interaction_type = 'LIKE'), " +
                "dislikes_count = (SELECT count(*) FROM interactions WHERE post_id = ? AND interaction_type = 'DISLIKE') " +
                "WHERE id = ?",
                postId, postId, postId
            );
            jdbcTemplate.update(
                "UPDATE post_options SET votes_count = (SELECT count(*) FROM interactions WHERE option_id = post_options.id AND interaction_type = 'VOTE') WHERE post_id = ?",
                postId
            );
        }

        // 4. Zera o vetor comportamental (40% do algoritmo)
        jdbcTemplate.update("DELETE FROM behavior_profile_attributes WHERE user_id = ?", userId);

        // 5. Zera o vetor explícito do quiz (60% do algoritmo)
        jdbcTemplate.update("DELETE FROM user_profile_attributes WHERE user_id = ?", userId);

        // 6. Remove o histórico de respostas do quiz e o orçamento do perfil
        jdbcTemplate.update("DELETE FROM quiz_responses WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM user_profiles WHERE user_id = ?", userId);
    }
}
