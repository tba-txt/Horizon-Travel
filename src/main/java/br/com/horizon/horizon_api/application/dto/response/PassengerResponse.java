package br.com.horizon.horizon_api.application.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PassengerResponse {
    private Long id;
    private String name;
    private Integer age;
}
