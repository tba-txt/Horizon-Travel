package br.com.horizon.horizon_api.application.usecase;

import br.com.horizon.horizon_api.application.dto.response.UserResponse;
import org.springframework.web.multipart.MultipartFile;

public interface UploadProfilePictureUseCase {
    UserResponse execute(Long userId, MultipartFile file);
}
