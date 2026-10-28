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
    public ResponseEntity<byte[]> downloadPdf(
            @PathVariable String ticketNumber,
            @RequestParam(value = "seatClass", required = false) String seatClassParam
    ) {
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
                return generatePdfResponse(ticket, res, seatClassParam);
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
        return generatePdfResponse(ticket, res, seatClassParam);
    }

    private ResponseEntity<byte[]> generatePdfResponse(Ticket ticket, Reservation res, String seatClassParam) {
        try (java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            com.itextpdf.text.Document document = new com.itextpdf.text.Document(com.itextpdf.text.PageSize.A4, 34, 34, 32, 32);
            com.itextpdf.text.pdf.PdfWriter writer = com.itextpdf.text.pdf.PdfWriter.getInstance(document, out);
            document.open();

            // Paleta de Cores Oficial Horizon (Estrita: Azul #5BA4D8 e Verde #65A30D)
            com.itextpdf.text.BaseColor horizonBlue = new com.itextpdf.text.BaseColor(91, 164, 216);       // #5BA4D8
            com.itextpdf.text.BaseColor horizonBlueLight = new com.itextpdf.text.BaseColor(240, 249, 255); // #F0F9FF
            com.itextpdf.text.BaseColor horizonGreen = new com.itextpdf.text.BaseColor(101, 163, 13);      // #65A30D
            com.itextpdf.text.BaseColor horizonGreenLight = new com.itextpdf.text.BaseColor(247, 254, 231);// #F7FEE7
            com.itextpdf.text.BaseColor slateDark = new com.itextpdf.text.BaseColor(30, 41, 59);           // #1E293B
            com.itextpdf.text.BaseColor slateMedium = new com.itextpdf.text.BaseColor(71, 85, 105);        // #475569
            com.itextpdf.text.BaseColor slateMuted = new com.itextpdf.text.BaseColor(100, 116, 139);       // #64748B
            com.itextpdf.text.BaseColor cardBg = new com.itextpdf.text.BaseColor(248, 250, 252);           // #F8FAFC
            com.itextpdf.text.BaseColor borderSoft = new com.itextpdf.text.BaseColor(226, 232, 240);       // #E2E8F0

            // Tipografia PDF com Cores Oficiais Horizon
            com.itextpdf.text.Font brandTitleFont = com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA_BOLD, 22, com.itextpdf.text.BaseColor.WHITE);
            com.itextpdf.text.Font brandSubFont = com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA_BOLD, 8.5f, horizonBlueLight);
            com.itextpdf.text.Font badgeFont = com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA_BOLD, 8.5f, com.itextpdf.text.BaseColor.WHITE);
            com.itextpdf.text.Font locatorLabelFont = com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA_BOLD, 7.5f, horizonBlueLight);
            com.itextpdf.text.Font locatorValueFont = com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA_BOLD, 11f, com.itextpdf.text.BaseColor.WHITE);

            com.itextpdf.text.Font labelFont = com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA_BOLD, 7.5f, slateMuted);
            com.itextpdf.text.Font valueFont = com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA_BOLD, 11f, slateDark);
            com.itextpdf.text.Font valueHighlightFont = com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA_BOLD, 12.5f, horizonBlue);
            com.itextpdf.text.Font classHighlightFont = com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA_BOLD, 11f, horizonGreen);
            com.itextpdf.text.Font iataFont = com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA_BOLD, 24f, horizonBlue);
            com.itextpdf.text.Font citySubFont = com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA, 8.5f, slateMedium);
            com.itextpdf.text.Font routeTagFont = com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA_BOLD, 8f, horizonGreen);
            com.itextpdf.text.Font sectionHeaderFont = com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA_BOLD, 9.5f, com.itextpdf.text.BaseColor.WHITE);
            com.itextpdf.text.Font smallInfoFont = com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA, 8.5f, slateMedium);
            com.itextpdf.text.Font footerFont = com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA, 8f, slateMuted);

            java.time.format.DateTimeFormatter dateFmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
            java.time.format.DateTimeFormatter timeFmt = java.time.format.DateTimeFormatter.ofPattern("HH:mm");
            java.time.format.DateTimeFormatter dateTimeFmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

            // Dados de Destino e Passageiro
            String pName = (ticket.getPassengerName() != null && !ticket.getPassengerName().isBlank())
                    ? ticket.getPassengerName().toUpperCase()
                    : "PASSAGEIRO HORIZON";
            String tNumber = ticket.getTicketNumber() != null ? ticket.getTicketNumber() : ("HZ-R" + res.getId());

            String destFullName = "Destino Horizon";
            String destCity = "Destino";
            if (res.getDestinationId() != null && destinationPort != null) {
                java.util.Optional<br.com.horizon.horizon_api.domain.model.Destination> dOpt = destinationPort.findById(res.getDestinationId());
                if (dOpt.isPresent()) {
                    br.com.horizon.horizon_api.domain.model.Destination d = dOpt.get();
                    destCity = (d.getCity() != null && !d.getCity().isBlank()) ? d.getCity() : d.getName();
                    destFullName = d.getName() + (d.getCountry() != null ? ", " + d.getCountry() : "");
                }
            }

            // =========================================================================
            // 1. CABEÇALHO INSTITUCIONAL HORIZON (AZUL PADRÃO #5BA4D8 + VERDE #65A30D)
            // =========================================================================
            com.itextpdf.text.pdf.PdfPTable headerTable = new com.itextpdf.text.pdf.PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{65f, 35f});
            headerTable.setSpacingAfter(12f);

            com.itextpdf.text.pdf.PdfPCell brandCell = new com.itextpdf.text.pdf.PdfPCell();
            brandCell.setBackgroundColor(horizonBlue);
            brandCell.setBorder(com.itextpdf.text.Rectangle.NO_BORDER);
            brandCell.setPaddingTop(16f);
            brandCell.setPaddingBottom(16f);
            brandCell.setPaddingLeft(18f);
            brandCell.setPaddingRight(10f);

            com.itextpdf.text.Paragraph brandTitle = new com.itextpdf.text.Paragraph("HORIZON", brandTitleFont);
            brandTitle.setSpacingAfter(3f);
            brandCell.addElement(brandTitle);

            com.itextpdf.text.Paragraph brandSub = new com.itextpdf.text.Paragraph("CARTÃO DE EMBARQUE DIGITAL  •  E-TICKET OFICIAL", brandSubFont);
            brandCell.addElement(brandSub);
            headerTable.addCell(brandCell);

            com.itextpdf.text.pdf.PdfPCell statusCell = new com.itextpdf.text.pdf.PdfPCell();
            statusCell.setBackgroundColor(horizonBlue);
            statusCell.setBorder(com.itextpdf.text.Rectangle.NO_BORDER);
            statusCell.setPaddingTop(14f);
            statusCell.setPaddingBottom(14f);
            statusCell.setPaddingRight(18f);
            statusCell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_RIGHT);

            // Badge Confirmado em Verde Padrão Horizon (#65A30D)
            com.itextpdf.text.pdf.PdfPTable badgeTable = new com.itextpdf.text.pdf.PdfPTable(1);
            badgeTable.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_RIGHT);
            badgeTable.setWidthPercentage(78);
            com.itextpdf.text.pdf.PdfPCell badgeInner = new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Phrase("RESERVA CONFIRMADA", badgeFont));
            badgeInner.setBackgroundColor(horizonGreen);
            badgeInner.setBorder(com.itextpdf.text.Rectangle.NO_BORDER);
            badgeInner.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
            badgeInner.setPaddingTop(4f);
            badgeInner.setPaddingBottom(5f);
            badgeTable.addCell(badgeInner);
            statusCell.addElement(badgeTable);

            com.itextpdf.text.Paragraph locLabel = new com.itextpdf.text.Paragraph("CÓDIGO DO BILHETE", locatorLabelFont);
            locLabel.setAlignment(com.itextpdf.text.Element.ALIGN_RIGHT);
            locLabel.setSpacingBefore(6f);
            statusCell.addElement(locLabel);

            com.itextpdf.text.Paragraph locVal = new com.itextpdf.text.Paragraph(tNumber, locatorValueFont);
            locVal.setAlignment(com.itextpdf.text.Element.ALIGN_RIGHT);
            statusCell.addElement(locVal);

            headerTable.addCell(statusCell);
            document.add(headerTable);

            // =========================================================================
            // 2. CARD DE DADOS DO PASSAGEIRO E DESTINO (EM GRID ESTRUTURADO)
            // =========================================================================
            com.itextpdf.text.pdf.PdfPTable paxCard = new com.itextpdf.text.pdf.PdfPTable(4);
            paxCard.setWidthPercentage(100);
            paxCard.setWidths(new float[]{34f, 32f, 16f, 18f});
            paxCard.setSpacingAfter(14f);

            paxCard.addCell(createMetadataCell("PASSAGEIRO TITULAR", pName, labelFont, valueHighlightFont, cardBg, borderSoft));
            paxCard.addCell(createMetadataCell("DESTINO SELECIONADO", destFullName, labelFont, valueFont, cardBg, borderSoft));
            paxCard.addCell(createMetadataCell("RESERVA", "#" + res.getId(), labelFont, valueFont, cardBg, borderSoft));

            String tripTypeLabel = (res.getTripType() == br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.TripType.ROUND_TRIP)
                    ? "Ida e Volta"
                    : "Somente Ida";
            paxCard.addCell(createMetadataCell("MODALIDADE", tripTypeLabel, labelFont, valueFont, cardBg, borderSoft));
            document.add(paxCard);

            boolean isRoundTrip = res.getTripType() == br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.TripType.ROUND_TRIP;

            // =========================================================================
            // 3. SEÇÃO DE VOO DE IDA (CARTÃO DE EMBARQUE)
            // =========================================================================
            if (res.getOutboundFlightId() != null && flightPort != null) {
                java.util.Optional<br.com.horizon.horizon_api.domain.model.Flight> outboundOpt = flightPort.getFlightById(res.getOutboundFlightId());
                if (outboundOpt.isPresent()) {
                    br.com.horizon.horizon_api.domain.model.Flight outbound = outboundOpt.get();
                    String outboundSeatClass = resolveDisplaySeatClass(seatClassParam, outbound, res.getTotalAmount(), res.getNumberOfPassengers(), isRoundTrip);
                    document.add(createFlightBoardingCard(
                            "TRECHO 01  •  VOO DE IDA",
                            outbound,
                            "São Paulo",
                            destCity,
                            res.getId(),
                            1,
                            outboundSeatClass,
                            horizonBlue,
                            horizonGreenLight,
                            horizonGreen,
                            cardBg,
                            borderSoft,
                            sectionHeaderFont,
                            iataFont,
                            citySubFont,
                            routeTagFont,
                            labelFont,
                            valueFont,
                            classHighlightFont,
                            dateFmt,
                            timeFmt
                    ));
                }
            }

            // =========================================================================
            // 4. SEÇÃO DE VOO DE VOLTA (SE APLICÁVEL)
            // =========================================================================
            if (isRoundTrip && res.getReturnFlightId() != null && flightPort != null) {
                java.util.Optional<br.com.horizon.horizon_api.domain.model.Flight> retOpt = flightPort.getFlightById(res.getReturnFlightId());
                if (retOpt.isPresent()) {
                    br.com.horizon.horizon_api.domain.model.Flight retFlight = retOpt.get();
                    String returnSeatClass = resolveDisplaySeatClass(seatClassParam, retFlight, res.getTotalAmount(), res.getNumberOfPassengers(), isRoundTrip);
                    document.add(createFlightBoardingCard(
                            "TRECHO 02  •  VOO DE VOLTA",
                            retFlight,
                            destCity,
                            "São Paulo",
                            res.getId(),
                            2,
                            returnSeatClass,
                            horizonGreen,
                            horizonGreenLight,
                            horizonGreen,
                            cardBg,
                            borderSoft,
                            sectionHeaderFont,
                            iataFont,
                            citySubFont,
                            routeTagFont,
                            labelFont,
                            valueFont,
                            classHighlightFont,
                            dateFmt,
                            timeFmt
                    ));
                }
            }

            // =========================================================================
            // 5. CANHOTO DE AUTENTICAÇÃO (CÓDIGO DE BARRAS OFICIAL + AVISOS DE EMBARQUE)
            // =========================================================================
            com.itextpdf.text.pdf.PdfPTable stubTable = new com.itextpdf.text.pdf.PdfPTable(2);
            stubTable.setWidthPercentage(100);
            stubTable.setWidths(new float[]{62f, 38f});
            stubTable.setSpacingBefore(4f);
            stubTable.setSpacingAfter(14f);

            // Coluna esquerda: Instruções de Embarque
            com.itextpdf.text.pdf.PdfPCell infoCell = new com.itextpdf.text.pdf.PdfPCell();
            infoCell.setBackgroundColor(horizonGreenLight);
            infoCell.setBorderColor(horizonGreen);
            infoCell.setBorderWidth(1f);
            infoCell.setPadding(12f);

            com.itextpdf.text.Font infoTitleFont = com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA_BOLD, 9f, horizonGreen);
            com.itextpdf.text.Paragraph infoHeader = new com.itextpdf.text.Paragraph("INFORMAÇÕES IMPORTANTES PARA O EMBARQUE", infoTitleFont);
            infoHeader.setSpacingAfter(5f);
            infoCell.addElement(infoHeader);

            com.itextpdf.text.Paragraph tip1 = new com.itextpdf.text.Paragraph("• Apresente-se no portão de embarque com antecedência mínima de 40 minutos.", smallInfoFont);
            tip1.setSpacingAfter(2f);
            infoCell.addElement(tip1);

            com.itextpdf.text.Paragraph tip2 = new com.itextpdf.text.Paragraph("• Tenha em mãos documento oficial original com foto (RG ou Passaporte válido).", smallInfoFont);
            tip2.setSpacingAfter(2f);
            infoCell.addElement(tip2);

            com.itextpdf.text.Paragraph tip3 = new com.itextpdf.text.Paragraph("• Franquia incluída: 1 item pessoal (até 10kg) + bagagem conforme tarifa contratada.", smallInfoFont);
            infoCell.addElement(tip3);

            stubTable.addCell(infoCell);

            // Coluna direita: Barcode 128 nativo iText em Azul Padrão Horizon (#5BA4D8)
            com.itextpdf.text.pdf.PdfPCell barcodeCell = new com.itextpdf.text.pdf.PdfPCell();
            barcodeCell.setBackgroundColor(com.itextpdf.text.BaseColor.WHITE);
            barcodeCell.setBorderColor(borderSoft);
            barcodeCell.setBorderWidth(1f);
            barcodeCell.setPadding(10f);
            barcodeCell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
            barcodeCell.setVerticalAlignment(com.itextpdf.text.Element.ALIGN_MIDDLE);

            com.itextpdf.text.Paragraph bcLabel = new com.itextpdf.text.Paragraph("AUTENTICAÇÃO ELETRÔNICA", labelFont);
            bcLabel.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
            bcLabel.setSpacingAfter(6f);
            barcodeCell.addElement(bcLabel);

            try {
                com.itextpdf.text.pdf.Barcode128 code128 = new com.itextpdf.text.pdf.Barcode128();
                code128.setCode(tNumber.replaceAll("[^A-Za-z0-9\\-]", ""));
                code128.setCodeType(com.itextpdf.text.pdf.Barcode128.CODE128);
                code128.setBarHeight(32f);
                code128.setX(1.05f);
                code128.setFont(null); // Oculta texto default embaixo para formatar limpo
                com.itextpdf.text.Image bcImg = code128.createImageWithBarcode(writer.getDirectContent(), com.itextpdf.text.BaseColor.BLACK, com.itextpdf.text.BaseColor.BLACK);
                bcImg.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
                barcodeCell.addElement(bcImg);
            } catch (Exception ignored) {}

            com.itextpdf.text.Font monoSmall = com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.COURIER_BOLD, 8.5f, com.itextpdf.text.BaseColor.BLACK);
            com.itextpdf.text.Paragraph bcCodeText = new com.itextpdf.text.Paragraph(tNumber, monoSmall);
            bcCodeText.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
            bcCodeText.setSpacingBefore(4f);
            barcodeCell.addElement(bcCodeText);

            stubTable.addCell(barcodeCell);
            document.add(stubTable);

            // =========================================================================
            // 6. RODAPÉ OFICIAL HORIZON
            // =========================================================================
            String issuedStr = (ticket.getGeneratedAt() != null)
                    ? ticket.getGeneratedAt().atZoneSameInstant(java.time.ZoneId.of("America/Sao_Paulo")).format(dateTimeFmt)
                    : java.time.OffsetDateTime.now().atZoneSameInstant(java.time.ZoneId.of("America/Sao_Paulo")).format(dateTimeFmt);

            com.itextpdf.text.Paragraph footer = new com.itextpdf.text.Paragraph(
                    "Documento emitido eletronicamente em " + issuedStr + " (Horário de Brasília)  •  Horizon\n" +
                    "Tenha uma excelente experiência de viagem com a Horizon!",
                    footerFont
            );
            footer.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
            document.add(footer);

            document.close();

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
            headers.setContentDisposition(org.springframework.http.ContentDisposition.attachment()
                    .filename(resolveTicketFileName(ticket, res))
                    .build());

            return new ResponseEntity<>(out.toByteArray(), headers, org.springframework.http.HttpStatus.OK);
        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF", e);
        }
    }

    private String resolveTicketFileName(Ticket ticket, Reservation res) {
        String pName = (ticket != null && ticket.getPassengerName() != null && !ticket.getPassengerName().isBlank())
                ? ticket.getPassengerName().trim()
                : (res != null && res.getPassengers() != null && !res.getPassengers().isEmpty() && res.getPassengers().get(0).getName() != null)
                ? res.getPassengers().get(0).getName().trim()
                : "Passageiro";

        pName = pName.replaceAll("[\\\\/:*?\"<>|]", "").replaceAll("\\s+", "_");

        String dateStr = "";
        if (res != null && res.getOutboundFlight() != null && res.getOutboundFlight().getFlightDate() != null) {
            dateStr = res.getOutboundFlight().getFlightDate().format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        } else if (ticket != null && ticket.getGeneratedAt() != null) {
            dateStr = ticket.getGeneratedAt().format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        } else {
            dateStr = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        }

        return "Passagem_Horizon_" + pName + "_" + dateStr + ".pdf";
    }

    private com.itextpdf.text.pdf.PdfPCell createMetadataCell(
            String label,
            String value,
            com.itextpdf.text.Font labelFont,
            com.itextpdf.text.Font valueFont,
            com.itextpdf.text.BaseColor bgColor,
            com.itextpdf.text.BaseColor borderColor
    ) {
        com.itextpdf.text.pdf.PdfPCell cell = new com.itextpdf.text.pdf.PdfPCell();
        cell.setBackgroundColor(bgColor);
        cell.setBorderColor(borderColor);
        cell.setBorderWidth(1f);
        cell.setPaddingTop(9f);
        cell.setPaddingBottom(10f);
        cell.setPaddingLeft(11f);
        cell.setPaddingRight(11f);

        com.itextpdf.text.Paragraph pLabel = new com.itextpdf.text.Paragraph(label, labelFont);
        pLabel.setSpacingAfter(3f);
        cell.addElement(pLabel);

        com.itextpdf.text.Paragraph pVal = new com.itextpdf.text.Paragraph(value != null ? value : "-", valueFont);
        cell.addElement(pVal);
        return cell;
    }

    private String resolveDisplaySeatClass(String seatClassParam, br.com.horizon.horizon_api.domain.model.Flight flight, java.math.BigDecimal totalAmount, Integer passengers, boolean isRoundTrip) {
        if (seatClassParam != null && !seatClassParam.isBlank()) {
            String normalized = seatClassParam.trim().toUpperCase();
            if (normalized.contains("PREMIUM")) return "Premium";
            if (normalized.contains("EXECUTIV")) return "Executivo";
            if (normalized.contains("BASIC") || normalized.contains("BÁSIC") || normalized.contains("BASICO")) return "Básico";
        }
        return deduceDisplaySeatClass(flight, totalAmount, passengers, isRoundTrip);
    }

    private String deduceDisplaySeatClass(br.com.horizon.horizon_api.domain.model.Flight flight, java.math.BigDecimal totalAmount, Integer passengers, boolean isRoundTrip) {
        if (flight == null) return "Básico";
        if (totalAmount != null && passengers != null && passengers > 0) {
            java.math.BigDecimal perPersonTotal = totalAmount.divide(java.math.BigDecimal.valueOf(passengers), 2, java.math.RoundingMode.HALF_UP);
            java.math.BigDecimal legPrice = isRoundTrip 
                    ? perPersonTotal.divide(new java.math.BigDecimal("2"), 2, java.math.RoundingMode.HALF_UP)
                    : perPersonTotal;

            if (flight.getPricePremium() != null && flight.getPricePremium().compareTo(legPrice) == 0) {
                return "Premium";
            }
            if (flight.getPriceExecutive() != null && flight.getPriceExecutive().compareTo(legPrice) == 0) {
                return "Executivo";
            }
            if (flight.getPricePerPerson() != null && flight.getPricePerPerson().compareTo(legPrice) == 0) {
                return "Básico";
            }

            // Comparação por proximidade para evitar divergências de arredondamento
            if (flight.getPricePremium() != null && flight.getPriceExecutive() != null) {
                java.math.BigDecimal diffPrem = flight.getPricePremium().subtract(legPrice).abs();
                java.math.BigDecimal diffExec = flight.getPriceExecutive().subtract(legPrice).abs();
                java.math.BigDecimal diffBasic = flight.getPricePerPerson() != null ? flight.getPricePerPerson().subtract(legPrice).abs() : java.math.BigDecimal.valueOf(999999);
                if (diffPrem.compareTo(diffExec) < 0 && diffPrem.compareTo(diffBasic) < 0) return "Premium";
                if (diffExec.compareTo(diffPrem) < 0 && diffExec.compareTo(diffBasic) < 0) return "Executivo";
            }
        }
        return "Básico";
    }

    private com.itextpdf.text.pdf.PdfPTable createFlightBoardingCard(
            String sectionTitle,
            br.com.horizon.horizon_api.domain.model.Flight flight,
            String originCityName,
            String destCityName,
            Long reservationId,
            int legNumber,
            String chosenSeatClass,
            com.itextpdf.text.BaseColor headerColor,
            com.itextpdf.text.BaseColor routeBadgeBg,
            com.itextpdf.text.BaseColor routeBadgeBorderColor,
            com.itextpdf.text.BaseColor detailsBg,
            com.itextpdf.text.BaseColor borderColor,
            com.itextpdf.text.Font sectionHeaderFont,
            com.itextpdf.text.Font iataFont,
            com.itextpdf.text.Font citySubFont,
            com.itextpdf.text.Font routeTagFont,
            com.itextpdf.text.Font labelFont,
            com.itextpdf.text.Font valueFont,
            com.itextpdf.text.Font classHighlightFont,
            java.time.format.DateTimeFormatter dateFmt,
            java.time.format.DateTimeFormatter timeFmt
    ) throws com.itextpdf.text.DocumentException {

        com.itextpdf.text.pdf.PdfPTable cardWrapper = new com.itextpdf.text.pdf.PdfPTable(1);
        cardWrapper.setWidthPercentage(100);
        cardWrapper.setSpacingAfter(14f);

        // 1. Barra de título do trecho
        com.itextpdf.text.pdf.PdfPTable topBar = new com.itextpdf.text.pdf.PdfPTable(2);
        topBar.setWidthPercentage(100);
        topBar.setWidths(new float[]{65f, 35f});

        com.itextpdf.text.pdf.PdfPCell titleCell = new com.itextpdf.text.pdf.PdfPCell(
                new com.itextpdf.text.Phrase(sectionTitle, sectionHeaderFont)
        );
        titleCell.setBackgroundColor(headerColor);
        titleCell.setBorder(com.itextpdf.text.Rectangle.NO_BORDER);
        titleCell.setPaddingTop(7f);
        titleCell.setPaddingBottom(7f);
        titleCell.setPaddingLeft(14f);
        topBar.addCell(titleCell);

        com.itextpdf.text.pdf.PdfPCell flightNoHeaderCell = new com.itextpdf.text.pdf.PdfPCell(
                new com.itextpdf.text.Phrase("VOO " + (flight.getFlightNumber() != null ? flight.getFlightNumber() : "HZ"), sectionHeaderFont)
        );
        flightNoHeaderCell.setBackgroundColor(headerColor);
        flightNoHeaderCell.setBorder(com.itextpdf.text.Rectangle.NO_BORDER);
        flightNoHeaderCell.setPaddingTop(7f);
        flightNoHeaderCell.setPaddingBottom(7f);
        flightNoHeaderCell.setPaddingRight(14f);
        flightNoHeaderCell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_RIGHT);
        topBar.addCell(flightNoHeaderCell);

        com.itextpdf.text.pdf.PdfPCell topBarContainer = new com.itextpdf.text.pdf.PdfPCell(topBar);
        topBarContainer.setBorder(com.itextpdf.text.Rectangle.NO_BORDER);
        topBarContainer.setPadding(0f);
        cardWrapper.addCell(topBarContainer);

        // 2. Painel Visual de Rota (ORIGEM e DESTINO sem setas)
        com.itextpdf.text.pdf.PdfPTable routeGrid = new com.itextpdf.text.pdf.PdfPTable(3);
        routeGrid.setWidthPercentage(100);
        routeGrid.setWidths(new float[]{38f, 24f, 38f});

        String origCode = flight.getOriginAirportCode() != null ? flight.getOriginAirportCode() : "GRU";
        String destCode = flight.getDestinationAirportCode() != null ? flight.getDestinationAirportCode() : "DST";

        java.time.LocalTime depTime = flight.getDepartureTime() != null ? flight.getDepartureTime() : java.time.LocalTime.of(8, 0);
        java.time.LocalTime arrTime = flight.getArrivalTime() != null ? flight.getArrivalTime() : depTime.plusHours(3).plusMinutes(15);
        java.time.LocalTime boardingTime = depTime.minusMinutes(40);

        // Origem
        com.itextpdf.text.pdf.PdfPCell originCell = new com.itextpdf.text.pdf.PdfPCell();
        originCell.setBackgroundColor(com.itextpdf.text.BaseColor.WHITE);
        originCell.setBorder(com.itextpdf.text.Rectangle.NO_BORDER);
        originCell.setPaddingTop(12f);
        originCell.setPaddingBottom(12f);
        originCell.setPaddingLeft(16f);

        com.itextpdf.text.Paragraph pOrigLabel = new com.itextpdf.text.Paragraph("ORIGEM / PARTIDA", labelFont);
        originCell.addElement(pOrigLabel);

        com.itextpdf.text.Paragraph pOrigIata = new com.itextpdf.text.Paragraph(origCode, iataFont);
        pOrigIata.setSpacingAfter(2f);
        originCell.addElement(pOrigIata);

        com.itextpdf.text.Paragraph pOrigCity = new com.itextpdf.text.Paragraph(originCityName + "  •  " + depTime.format(timeFmt), citySubFont);
        originCell.addElement(pOrigCity);
        routeGrid.addCell(originCell);

        // Centro da Rota
        com.itextpdf.text.pdf.PdfPCell centerRouteCell = new com.itextpdf.text.pdf.PdfPCell();
        centerRouteCell.setBackgroundColor(com.itextpdf.text.BaseColor.WHITE);
        centerRouteCell.setBorder(com.itextpdf.text.Rectangle.NO_BORDER);
        centerRouteCell.setVerticalAlignment(com.itextpdf.text.Element.ALIGN_MIDDLE);
        centerRouteCell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
        centerRouteCell.setPadding(8f);

        com.itextpdf.text.pdf.PdfPTable routeBadge = new com.itextpdf.text.pdf.PdfPTable(1);
        routeBadge.setWidthPercentage(90);
        com.itextpdf.text.pdf.PdfPCell rbCell = new com.itextpdf.text.pdf.PdfPCell(
                new com.itextpdf.text.Phrase("VOO DIRETO", routeTagFont)
        );
        rbCell.setBackgroundColor(routeBadgeBg);
        rbCell.setBorderColor(routeBadgeBorderColor);
        rbCell.setBorderWidth(1f);
        rbCell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
        rbCell.setPaddingTop(5f);
        rbCell.setPaddingBottom(5f);
        routeBadge.addCell(rbCell);
        centerRouteCell.addElement(routeBadge);

        com.itextpdf.text.Paragraph classSub = new com.itextpdf.text.Paragraph("OPERADO POR HORIZON", labelFont);
        classSub.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
        classSub.setSpacingBefore(4f);
        centerRouteCell.addElement(classSub);
        routeGrid.addCell(centerRouteCell);

        // Destino
        com.itextpdf.text.pdf.PdfPCell destCell = new com.itextpdf.text.pdf.PdfPCell();
        destCell.setBackgroundColor(com.itextpdf.text.BaseColor.WHITE);
        destCell.setBorder(com.itextpdf.text.Rectangle.NO_BORDER);
        destCell.setPaddingTop(12f);
        destCell.setPaddingBottom(12f);
        destCell.setPaddingRight(16f);
        destCell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_RIGHT);

        com.itextpdf.text.Paragraph pDestLabel = new com.itextpdf.text.Paragraph("DESTINO / CHEGADA", labelFont);
        pDestLabel.setAlignment(com.itextpdf.text.Element.ALIGN_RIGHT);
        destCell.addElement(pDestLabel);

        com.itextpdf.text.Paragraph pDestIata = new com.itextpdf.text.Paragraph(destCode, iataFont);
        pDestIata.setAlignment(com.itextpdf.text.Element.ALIGN_RIGHT);
        pDestIata.setSpacingAfter(2f);
        destCell.addElement(pDestIata);

        com.itextpdf.text.Paragraph pDestCity = new com.itextpdf.text.Paragraph(destCityName + "  •  " + arrTime.format(timeFmt), citySubFont);
        pDestCity.setAlignment(com.itextpdf.text.Element.ALIGN_RIGHT);
        destCell.addElement(pDestCity);
        routeGrid.addCell(destCell);

        com.itextpdf.text.pdf.PdfPCell routeContainer = new com.itextpdf.text.pdf.PdfPCell(routeGrid);
        routeContainer.setBorderColor(borderColor);
        routeContainer.setBorderWidthLeft(1f);
        routeContainer.setBorderWidthRight(1f);
        routeContainer.setBorderWidthTop(0f);
        routeContainer.setBorderWidthBottom(0f);
        routeContainer.setPadding(0f);
        cardWrapper.addCell(routeContainer);

        // 3. Barra de Especificações de Embarque (Data, Início Embarque, Portão, Assento, Classe)
        com.itextpdf.text.pdf.PdfPTable specsGrid = new com.itextpdf.text.pdf.PdfPTable(5);
        specsGrid.setWidthPercentage(100);
        specsGrid.setWidths(new float[]{22f, 22f, 18f, 18f, 20f});

        String formattedDate = flight.getFlightDate() != null ? flight.getFlightDate().format(dateFmt) : "--/--/----";
        long seed = (reservationId != null ? reservationId : 1L) + legNumber;
        String gate = "B0" + ((seed % 8) + 1);
        String seat = ((seed % 18) + 2) + (legNumber == 1 ? "A" : "F");

        specsGrid.addCell(createMetadataCell("DATA DO VOO", formattedDate, labelFont, valueFont, detailsBg, borderColor));
        specsGrid.addCell(createMetadataCell("INÍCIO EMBARQUE", boardingTime.format(timeFmt), labelFont, valueFont, detailsBg, borderColor));
        specsGrid.addCell(createMetadataCell("PORTÃO / TERMINAL", gate + "  (T2)", labelFont, valueFont, detailsBg, borderColor));
        specsGrid.addCell(createMetadataCell("ASSENTO", seat, labelFont, valueFont, detailsBg, borderColor));
        specsGrid.addCell(createMetadataCell("CLASSE", chosenSeatClass, labelFont, classHighlightFont, detailsBg, borderColor));

        com.itextpdf.text.pdf.PdfPCell specsContainer = new com.itextpdf.text.pdf.PdfPCell(specsGrid);
        specsContainer.setBorder(com.itextpdf.text.Rectangle.NO_BORDER);
        specsContainer.setPadding(0f);
        cardWrapper.addCell(specsContainer);

        return cardWrapper;
    }
}
