package br.com.horizon.horizon_api.application.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PassengerRequest {
    @NotBlank(message = "Passenger name is required")
    private String name;
    
    @NotNull(message = "Passenger age is required")
    @Min(value = 1, message = "Age must be at least 1")
    @Max(value = 99, message = "Age must be at most 99")
    private Integer age;
}
