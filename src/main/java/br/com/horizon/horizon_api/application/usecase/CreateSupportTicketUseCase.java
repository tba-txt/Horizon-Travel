package br.com.horizon.horizon_api.application.usecase;

import br.com.horizon.horizon_api.application.dto.request.SupportTicketRequest;
import br.com.horizon.horizon_api.application.dto.response.SupportTicketResponse;
import br.com.horizon.horizon_api.application.service.EmailTemplateService;
import br.com.horizon.horizon_api.domain.model.SupportTicket;
import br.com.horizon.horizon_api.domain.port.EmailPort;
import br.com.horizon.horizon_api.domain.port.SupportTicketPersistencePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class CreateSupportTicketUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreateSupportTicketUseCase.class);

    private final SupportTicketPersistencePort persistencePort;
    private final EmailPort emailPort;

    public CreateSupportTicketUseCase(SupportTicketPersistencePort persistencePort, EmailPort emailPort) {
        this.persistencePort = persistencePort;
        this.emailPort = emailPort;
    }

    public SupportTicketResponse execute(SupportTicketRequest request, Long authenticatedUserId) {
        // Gera o código do protocolo sequencial: HZ-2026-01, HZ-2026-02, etc.
        long currentCount = persistencePort.count();
        long nextIndex = currentCount + 1;
        String ticketCode = String.format("HZ-2026-%02d", nextIndex);

        int slaHours = request.getSlaHours() != null ? request.getSlaHours() : calculateDefaultSlaHours(request.getSubject());
        String slaDescription = (request.getSlaDescription() != null && !request.getSlaDescription().isBlank())
                ? request.getSlaDescription()
                : ("até " + slaHours + " horas úteis");

        SupportTicket ticket = new SupportTicket();
        ticket.setTicketCode(ticketCode);
        ticket.setUserId(authenticatedUserId);
        ticket.setUserName(request.getUserName().trim());
        ticket.setUserEmail(request.getUserEmail().trim().toLowerCase());
        ticket.setSubject(request.getSubject().trim());
        ticket.setSlaHours(slaHours);
        ticket.setSlaDescription(slaDescription);
        ticket.setDescription(request.getDescription().trim());
        ticket.setAttachmentName(request.getAttachmentName());
        ticket.setAttachmentSize(request.getAttachmentSize());
        ticket.setAttachmentType(request.getAttachmentType());
        ticket.setAttachmentData(request.getAttachmentData());
        ticket.setStatus("EM_ATENDIMENTO");
        ticket.setCreatedAt(OffsetDateTime.now());
        ticket.setUpdatedAt(OffsetDateTime.now());

        SupportTicket saved = persistencePort.save(ticket);

        // Dispara os e-mails de forma assíncrona para responder instantaneamente ao cliente na UI
        final String finalTicketCode = ticketCode;
        final String clientEmail = saved.getUserEmail();
        final String clientName = saved.getUserName();
        final String ticketSubject = saved.getSubject();
        final String ticketSlaDesc = saved.getSlaDescription();
        final String ticketDesc = saved.getDescription();

        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                String subjectLine = "Horizon - Chamado Aberto (" + finalTicketCode + ")";
                String emailHtml = EmailTemplateService.buildSupportTicketEmail(
                        clientName,
                        finalTicketCode,
                        ticketSubject,
                        ticketSlaDesc,
                        ticketDesc
                );
                emailPort.sendEmail(clientEmail, subjectLine, emailHtml);
                log.info("E-mail de confirmação do chamado {} enviado para {}", finalTicketCode, clientEmail);

                // Envia também para a caixa oficial de suporte da Horizon
                try {
                    String adminSubject = "[Novo Chamado " + finalTicketCode + "] " + ticketSubject + " - " + clientName;
                    emailPort.sendEmail("horiozonfatecpi@gmail.com", adminSubject, emailHtml);
                    log.info("Cópia do chamado {} enviada para horiozonfatecpi@gmail.com", finalTicketCode);
                } catch (Exception adminEx) {
                    log.warn("Não foi possível enviar cópia do chamado para horiozonfatecpi@gmail.com: {}", adminEx.getMessage());
                }
            } catch (Exception ex) {
                log.warn("Não foi possível enviar o e-mail do chamado {}: {}", finalTicketCode, ex.getMessage());
            }
        });

        return toResponse(saved, false);
    }

    private int calculateDefaultSlaHours(String subject) {
        if (subject == null) return 24;
        String s = subject.toLowerCase();
        if (s.contains("pagamento") || s.contains("pix") || s.contains("cartão")) return 2;
        if (s.contains("bagagem") || s.contains("assento")) return 4;
        if (s.contains("dados") || s.contains("passageiro") || s.contains("classe") || s.contains("cupom")) return 6;
        if (s.contains("documentação") || s.contains("visto") || s.contains("passaporte")) return 8;
        if (s.contains("alteração") || s.contains("data") || s.contains("voo")) return 12;
        if (s.contains("cancelamento") || s.contains("reembolso") || s.contains("nota")) return 24;
        return 48;
    }

    public static SupportTicketResponse toResponse(SupportTicket ticket) {
        return toResponse(ticket, true);
    }

    public static SupportTicketResponse toResponse(SupportTicket ticket, boolean includeAttachmentData) {
        if (ticket == null) return null;
        return SupportTicketResponse.builder()
                .id(ticket.getId())
                .ticketCode(ticket.getTicketCode())
                .userId(ticket.getUserId())
                .userName(ticket.getUserName())
                .userEmail(ticket.getUserEmail())
                .subject(ticket.getSubject())
                .slaHours(ticket.getSlaHours())
                .slaDescription(ticket.getSlaDescription())
                .description(ticket.getDescription())
                .attachmentName(ticket.getAttachmentName())
                .attachmentSize(ticket.getAttachmentSize())
                .attachmentType(ticket.getAttachmentType())
                .attachmentData(includeAttachmentData ? ticket.getAttachmentData() : null)
                .status(ticket.getStatus())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .build();
    }
}
