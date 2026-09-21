package br.com.horizon.horizon_api.application.dto.response;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Setter
public class UserResponse {
    private Long id;
    private String name;
    private String socialName;
    private String email;
    private String cpf;
    private LocalDate birthDate;
    private String profileImageUrl;
    private java.math.BigDecimal budgetPerPerson;
    private Boolean active;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
