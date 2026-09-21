package br.com.horizon.horizon_api.application.usecase;

import java.util.List;
import br.com.horizon.horizon_api.application.dto.response.DestinationResponse;

public interface ListDestinationsUseCase {
    List<DestinationResponse> execute();
}
