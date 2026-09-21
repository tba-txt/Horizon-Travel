package br.com.horizon.horizon_api.application.dto.response;
import lombok.Getter;
import lombok.Setter;
import java.util.List;
@Getter @Setter
public class QuizResponseDTO {
    private Long id;
    private String title;
    private Integer version;
    private List<QuestionResponseDTO> questions;
}
