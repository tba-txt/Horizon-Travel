package br.com.horizon.horizon_api.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class AnswerAttributeId implements Serializable {

    @Column(name = "answer_id")
    private Long answerId;

    @Column(name = "attribute_id")
    private Long attributeId;
}
