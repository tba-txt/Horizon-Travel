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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketPersistencePort ticketPort;
    private final ReservationPersistencePort reservationPort;
    private final br.com.horizon.horizon_api.domain.port.FlightPersistencePort flightPort;
    private final br.com.horizon.horizon_api.domain.port.DestinationPersistencePort destinationPort;
    private final TicketMapper ticketMapper;
    private final GetReservationTicketsUseCase getReservationTicketsUseCase;

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

    @GetMapping("/reservation/{reservationId}")
    public ResponseEntity<List<TicketResponse>> getByReservationId(@PathVariable Long reservationId) {
        Long userId = SecurityUtils.getAuthenticatedUserId();
        return ResponseEntity.ok(getReservationTicketsUseCase.execute(reservationId, userId));
    }

    @GetMapping("/{ticketNumber}/download")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable String ticketNumber) {
        Long userId = SecurityUtils.getAuthenticatedUserId();
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

        try (java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            com.itextpdf.text.Document document = new com.itextpdf.text.Document();
            com.itextpdf.text.pdf.PdfWriter.getInstance(document, out);
            document.open();
            
            com.itextpdf.text.Font titleFont = com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA_BOLD, 18);
            com.itextpdf.text.Font sectionFont = com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA_BOLD, 13);
            com.itextpdf.text.Font textFont = com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA, 11);
            
            document.add(new com.itextpdf.text.Paragraph("HORIZON TRAVEL - CARTAO DE EMBARQUE", titleFont));
            document.add(new com.itextpdf.text.Paragraph("==================================================", textFont));
            
            // Passenger & Ticket Info
            document.add(new com.itextpdf.text.Paragraph("Passageiro: " + ticket.getPassengerName(), textFont));
            document.add(new com.itextpdf.text.Paragraph("Numero do Bilhete: " + ticket.getTicketNumber(), textFont));
            document.add(new com.itextpdf.text.Paragraph("Codigo da Reserva: #" + ticket.getReservationId(), textFont));
            
            if (res.getDestinationId() != null && destinationPort != null) {
                destinationPort.findById(res.getDestinationId()).ifPresent(dest -> {
                    try {
                        document.add(new com.itextpdf.text.Paragraph("Destino: " + dest.getName() + ", " + dest.getCountry(), textFont));
                    } catch (com.itextpdf.text.DocumentException ignored) {}
                });
            }
            
            document.add(new com.itextpdf.text.Paragraph("Tipo de Viagem: " + res.getTripType(), textFont));
            document.add(new com.itextpdf.text.Paragraph("--------------------------------------------------", textFont));

            // Outbound Flight Details
            if (res.getOutboundFlightId() != null && flightPort != null) {
                flightPort.getFlightById(res.getOutboundFlightId()).ifPresent(outbound -> {
                    try {
                        document.add(new com.itextpdf.text.Paragraph("VOO DE IDA", sectionFont));
                        document.add(new com.itextpdf.text.Paragraph("Voo: " + outbound.getFlightNumber(), textFont));
                        document.add(new com.itextpdf.text.Paragraph("Origem: " + outbound.getOriginAirportCode() + " -> Destino: " + outbound.getDestinationAirportCode(), textFont));
                        document.add(new com.itextpdf.text.Paragraph("Data: " + outbound.getFlightDate(), textFont));
                        document.add(new com.itextpdf.text.Paragraph("Horario de Partida: " + outbound.getDepartureTime() + " | Horario de Chegada: " + outbound.getArrivalTime(), textFont));
                        document.add(new com.itextpdf.text.Paragraph("--------------------------------------------------", textFont));
                    } catch (com.itextpdf.text.DocumentException ignored) {}
                });
            }

            // Return Flight Details (if applicable)
            if (res.getTripType() == br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.TripType.ROUND_TRIP && 
                res.getReturnFlightId() != null && flightPort != null) {
                flightPort.getFlightById(res.getReturnFlightId()).ifPresent(retFlight -> {
                    try {
                        document.add(new com.itextpdf.text.Paragraph("VOO DE VOLTA", sectionFont));
                        document.add(new com.itextpdf.text.Paragraph("Voo: " + retFlight.getFlightNumber(), textFont));
                        document.add(new com.itextpdf.text.Paragraph("Origem: " + retFlight.getOriginAirportCode() + " -> Destino: " + retFlight.getDestinationAirportCode(), textFont));
                        document.add(new com.itextpdf.text.Paragraph("Data: " + retFlight.getFlightDate(), textFont));
                        document.add(new com.itextpdf.text.Paragraph("Horario de Partida: " + retFlight.getDepartureTime() + " | Horario de Chegada: " + retFlight.getArrivalTime(), textFont));
                        document.add(new com.itextpdf.text.Paragraph("--------------------------------------------------", textFont));
                    } catch (com.itextpdf.text.DocumentException ignored) {}
                });
            }

            document.add(new com.itextpdf.text.Paragraph("Bilhete emitido em: " + ticket.getGeneratedAt(), textFont));
            document.add(new com.itextpdf.text.Paragraph("Tenha uma excelente viagem com a Horizon!", textFont));
            
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
