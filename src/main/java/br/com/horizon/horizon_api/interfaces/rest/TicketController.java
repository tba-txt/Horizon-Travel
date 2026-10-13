package br.com.horizon.horizon_api.interfaces.rest;

import br.com.horizon.horizon_api.application.dto.response.TicketResponse;
import br.com.horizon.horizon_api.application.mapper.TicketMapper;
import br.com.horizon.horizon_api.application.usecase.GetReservationTicketsUseCase;
import br.com.horizon.horizon_api.domain.model.Reservation;
import br.com.horizon.horizon_api.domain.model.Ticket;
import br.com.horizon.horizon_api.domain.port.ReservationPersistencePort;
import br.com.horizon.horizon_api.domain.port.TicketPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
@Tag(name = "Bilhetes", description = "Endpoints para consulta e download de bilhetes/passagens")
public class TicketController {

    private final TicketPersistencePort ticketPort;
    private final ReservationPersistencePort reservationPort;
    private final br.com.horizon.horizon_api.domain.port.FlightPersistencePort flightPort;
    private final br.com.horizon.horizon_api.domain.port.DestinationPersistencePort destinationPort;
    private final TicketMapper ticketMapper;
    private final GetReservationTicketsUseCase getReservationTicketsUseCase;

    @Operation(summary = "Listar meus bilhetes", description = "Retorna todos os bilhetes emitidos das viagens do usuário autenticado.")
    @GetMapping("/my")
    public ResponseEntity<List<TicketResponse>> getMyTickets() {
        Long userId = SecurityUtils.getAuthenticatedUserId();
        List<Reservation> userReservations = reservationPort.findByUserId(userId);
        List<TicketResponse> allTickets = new java.util.ArrayList<>();
        for (Reservation res : userReservations) {
            List<Ticket> tickets = ticketPort.findByReservationId(res.getId());
            for (Ticket t : tickets) {
                allTickets.add(ticketMapper.toResponse(t));
            }
        }
        return ResponseEntity.ok(allTickets);
    }

    @Operation(summary = "Obter bilhete por ID", description = "Retorna os detalhes de um bilhete específico.")
    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> getById(@PathVariable Long id) {
        Long userId = SecurityUtils.getAuthenticatedUserId();
        return ticketPort.findById(id).map(ticket -> {
            java.util.Optional<Reservation> res = reservationPort.findByIdAndUserId(ticket.getReservationId(), userId);
            if (res.isEmpty()) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).<TicketResponse>build();
            }
            return ResponseEntity.ok(ticketMapper.toResponse(ticket));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Listar bilhetes por reserva", description = "Retorna os bilhetes de todos os passageiros de uma reserva específica.")
    @GetMapping("/reservation/{reservationId}")
    public ResponseEntity<List<TicketResponse>> getByReservationId(@PathVariable Long reservationId) {
        Long userId = SecurityUtils.getAuthenticatedUserId();
        return ResponseEntity.ok(getReservationTicketsUseCase.execute(reservationId, userId));
    }

    @Operation(summary = "Baixar bilhete em PDF", description = "Gera e baixa o cartão de embarque oficial em formato PDF.")
    @GetMapping("/{ticketNumber}/download")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<byte[]> downloadPdf(@PathVariable String ticketNumber) {
        Long userId = SecurityUtils.getAuthenticatedUserId();
        if (ticketNumber != null && ticketNumber.startsWith("RES-")) {
            try {
                Long resId = Long.parseLong(ticketNumber.substring(4));
                java.util.Optional<Reservation> resOpt = reservationPort.findByIdAndUserId(resId, userId);
                if (resOpt.isEmpty()) {
                    return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).build();
                }
                Reservation res = resOpt.get();
                List<Ticket> existingTickets = ticketPort.findByReservationId(res.getId());
                Ticket ticket;
                if (existingTickets != null && !existingTickets.isEmpty()) {
                    ticket = existingTickets.get(0);
                } else {
                    ticket = new Ticket();
                    ticket.setReservationId(res.getId());
                    ticket.setTicketNumber("HZ-" + java.time.LocalDate.now().getYear() + "-R" + res.getId());
                    if (res.getPassengers() != null && !res.getPassengers().isEmpty()) {
                        ticket.setPassengerName(res.getPassengers().get(0).getName());
                    } else {
                        ticket.setPassengerName("Passageiro Horizon");
                    }
                    ticket.setGeneratedAt(java.time.OffsetDateTime.now());
                }
                return generatePdfResponse(ticket, res);
            } catch (NumberFormatException ignored) {}
        }

        java.util.Optional<Ticket> ticketOpt = ticketPort.findByTicketNumber(ticketNumber);
        if (ticketOpt.isEmpty()) {
            try {
                Long numericId = Long.parseLong(ticketNumber);
                ticketOpt = ticketPort.findById(numericId);
            } catch (NumberFormatException ignored) {}
        }

        if (ticketOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Ticket ticket = ticketOpt.get();
                
        // Validation: user must own the reservation
        java.util.Optional<Reservation> resOpt = reservationPort.findByIdAndUserId(ticket.getReservationId(), userId);
        if (resOpt.isEmpty()) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).build();
        }
        Reservation res = resOpt.get();
        return generatePdfResponse(ticket, res);
    }

    private ResponseEntity<byte[]> generatePdfResponse(Ticket ticket, Reservation res) {

        try (java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            com.itextpdf.text.Document document = new com.itextpdf.text.Document();
            com.itextpdf.text.pdf.PdfWriter.getInstance(document, out);
            document.open();
            
            com.itextpdf.text.Font titleFont = com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA_BOLD, 24, new com.itextpdf.text.BaseColor(72, 149, 204));
            com.itextpdf.text.Font subtitleFont = com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA_BOLD, 14, com.itextpdf.text.BaseColor.DARK_GRAY);
            com.itextpdf.text.Font textFont = com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA, 11, com.itextpdf.text.BaseColor.BLACK);
            com.itextpdf.text.Font boldFont = com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA_BOLD, 12, com.itextpdf.text.BaseColor.BLACK);
            
            // Try to load a beautiful airplane banner image
            try {
                com.itextpdf.text.Image banner = com.itextpdf.text.Image.getInstance(new java.net.URL("https://images.unsplash.com/photo-1436491865332-7a61a109cc05?q=80&w=800&auto=format&fit=crop"));
                banner.scaleToFit(520, 150);
                banner.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
                document.add(banner);
            } catch (Exception ignored) {}

            document.add(new com.itextpdf.text.Paragraph(" "));
            
            com.itextpdf.text.Paragraph header = new com.itextpdf.text.Paragraph("HORIZON TRAVEL", titleFont);
            header.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
            document.add(header);
            
            com.itextpdf.text.Paragraph subHeader = new com.itextpdf.text.Paragraph("CARTÃO DE EMBARQUE OFICIAL", subtitleFont);
            subHeader.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
            document.add(subHeader);
            
            document.add(new com.itextpdf.text.Paragraph(" "));
            document.add(new com.itextpdf.text.Paragraph("---------------------------------------------------------------------------------------------------", textFont));
            
            // Passenger & Ticket Info
            String pName = (ticket.getPassengerName() != null && !ticket.getPassengerName().isBlank()) ? ticket.getPassengerName() : "Passageiro";
            document.add(new com.itextpdf.text.Paragraph("PASSAGEIRO: " + pName.toUpperCase(), boldFont));
            document.add(new com.itextpdf.text.Paragraph("Nº DO BILHETE: " + ticket.getTicketNumber(), textFont));
            document.add(new com.itextpdf.text.Paragraph("RESERVA: #" + ticket.getReservationId(), textFont));
            
            if (res.getDestinationId() != null && destinationPort != null) {
                destinationPort.findById(res.getDestinationId()).ifPresent(dest -> {
                    try {
                        document.add(new com.itextpdf.text.Paragraph("DESTINO: " + dest.getName() + ", " + dest.getCountry(), textFont));
                    } catch (com.itextpdf.text.DocumentException ignored) {}
                });
            }
            
            document.add(new com.itextpdf.text.Paragraph("---------------------------------------------------------------------------------------------------", textFont));

            // Outbound Flight Details
            if (res.getOutboundFlightId() != null && flightPort != null) {
                flightPort.getFlightById(res.getOutboundFlightId()).ifPresent(outbound -> {
                    try {
                        document.add(new com.itextpdf.text.Paragraph("✈ VOO DE IDA", subtitleFont));
                        document.add(new com.itextpdf.text.Paragraph("Voo: " + outbound.getFlightNumber() + " | " + outbound.getOriginAirportCode() + " ➔ " + outbound.getDestinationAirportCode(), boldFont));
                        document.add(new com.itextpdf.text.Paragraph("Data: " + outbound.getFlightDate() + " | Horário de Embarque: " + outbound.getDepartureTime(), textFont));
                        document.add(new com.itextpdf.text.Paragraph(" "));
                    } catch (com.itextpdf.text.DocumentException ignored) {}
                });
            }

            // Return Flight Details (if applicable)
            if (res.getTripType() == br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.TripType.ROUND_TRIP && 
                res.getReturnFlightId() != null && flightPort != null) {
                flightPort.getFlightById(res.getReturnFlightId()).ifPresent(retFlight -> {
                    try {
                        document.add(new com.itextpdf.text.Paragraph("✈ VOO DE VOLTA", subtitleFont));
                        document.add(new com.itextpdf.text.Paragraph("Voo: " + retFlight.getFlightNumber() + " | " + retFlight.getOriginAirportCode() + " ➔ " + retFlight.getDestinationAirportCode(), boldFont));
                        document.add(new com.itextpdf.text.Paragraph("Data: " + retFlight.getFlightDate() + " | Horário de Embarque: " + retFlight.getDepartureTime(), textFont));
                        document.add(new com.itextpdf.text.Paragraph(" "));
                    } catch (com.itextpdf.text.DocumentException ignored) {}
                });
            }

            document.add(new com.itextpdf.text.Paragraph("---------------------------------------------------------------------------------------------------", textFont));
            
            com.itextpdf.text.Font footerFont = com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA, 10, com.itextpdf.text.Font.ITALIC, com.itextpdf.text.BaseColor.GRAY);
            com.itextpdf.text.Paragraph footer = new com.itextpdf.text.Paragraph("Emitido em: " + ticket.getGeneratedAt() + "\nTenha uma excelente viagem com a Horizon Travel!", footerFont);
            footer.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
            document.add(footer);
            
            document.close();
            
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
            headers.setContentDisposition(org.springframework.http.ContentDisposition.attachment()
                    .filename("ticket_" + ticket.getTicketNumber() + ".pdf")
                    .build());
            
            return new ResponseEntity<>(out.toByteArray(), headers, org.springframework.http.HttpStatus.OK);
        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF", e);
        }
    }
}
