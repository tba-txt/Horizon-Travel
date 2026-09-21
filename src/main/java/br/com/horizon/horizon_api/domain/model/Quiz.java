package br.com.horizon.horizon_api.domain.model;
import lombok.Getter;
import lombok.Setter;
@Getter @Setter
public class Quiz {
    private Long id; private String title; private Integer version; private Boolean active; private java.time.OffsetDateTime createdAt;
}
