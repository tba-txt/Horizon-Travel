package br.com.horizon.horizon_api.domain.model;

import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;

@Getter
@Setter
public class Ticket {
    private Long id;
    private Long reservationId;
    private Long passengerId;
    private String passengerName;
    private String ticketNumber;
    private String pdfUrl;
    private OffsetDateTime generatedAt;
    private OffsetDateTime emailSentAt;
}
