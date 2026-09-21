package br.com.horizon.horizon_api.domain.model;

import br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.PaymentMethod;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.PaymentStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
public class Payment {
    private Long id;
    private Long reservationId;
    private PaymentMethod paymentMethod;
    private PaymentStatus status;
    private BigDecimal amount;
    private String paymentReference;
    private OffsetDateTime paidAt;
    private OffsetDateTime createdAt;
}
