package br.com.horizon.horizon_api.domain.model;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
public class UserProfile {
    private Long id;
    private Long userId;
    private BigDecimal budgetPerPerson;
    private Boolean hasPassport;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
