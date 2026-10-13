package br.com.horizon.horizon_api.domain.model;

import br.com.horizon.horizon_api.domain.exception.BusinessException;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

@Getter
@Setter
public class User {
    public static final int MINIMUM_REGISTRATION_AGE = 18;

    private Long id;
    private String name;
    private String socialName;
    private String email;
    private String passwordHash;
    private String cpf;
    private LocalDate birthDate;
    private String profileImageUrl;
    private Boolean active;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public int getAge() {
        if (this.birthDate == null) {
            return 0;
        }
        return (int) ChronoUnit.YEARS.between(this.birthDate, LocalDate.now());
    }

    public boolean isAdult() {
        return getAge() >= MINIMUM_REGISTRATION_AGE;
    }

    public void validateAge() {
        if (this.birthDate == null) {
            throw new BusinessException("A data de nascimento é obrigatória.");
        }
        if (this.birthDate.isAfter(LocalDate.now())) {
            throw new BusinessException("A data de nascimento não pode ser uma data futura.");
        }
        if (!isAdult()) {
            throw new BusinessException("O usuário deve ter pelo menos " + MINIMUM_REGISTRATION_AGE + " anos para se cadastrar.");
        }
    }
}
