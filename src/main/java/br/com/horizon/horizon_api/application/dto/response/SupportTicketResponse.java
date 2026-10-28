package br.com.horizon.horizon_api.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportTicketResponse {

    private Long id;
    private String ticketCode;
    private Long userId;
    private String userName;
    private String userEmail;
    private String subject;
    private Integer slaHours;
    private String slaDescription;
    private String description;
    private String attachmentName;
    private Long attachmentSize;
    private String attachmentType;
    private String attachmentData;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
