package br.com.horizon.horizon_api.domain.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Passenger {
    private Long id;
    private Long reservationId;
    private String name;
    private Integer age;
}
