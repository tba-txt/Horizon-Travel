package br.com.horizon.horizon_api.domain.model;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Setter
public class User {
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
}
