package br.com.horizon.horizon_api.application.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
public class TicketResponse {
    private Long id;
    private Long reservationId;
    private Long passengerId;
    private String passengerName;
    private String ticketNumber;
    private String pdfUrl;
    private OffsetDateTime generatedAt;
    private OffsetDateTime emailSentAt;
}
