package br.com.horizon.horizon_api.application.service;
import br.com.horizon.horizon_api.domain.model.*;
import br.com.horizon.horizon_api.domain.port.*;
import br.com.horizon.horizon_api.application.dto.response.ProfileResponseDTO;
import br.com.horizon.horizon_api.application.dto.response.ProfileAttributeDTO;
import br.com.horizon.horizon_api.application.usecase.GetExplicitProfileUseCase;
import br.com.horizon.horizon_api.application.usecase.GetBehaviorProfileUseCase;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfileService implements GetExplicitProfileUseCase, GetBehaviorProfileUseCase {
    private final UserProfileAttributePersistencePort explicitPort;
    private final BehaviorProfileAttributePersistencePort behaviorPort;
    
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
}
