package br.com.horizon.horizon_api.application.service;

import br.com.horizon.horizon_api.application.dto.response.TicketResponse;
import br.com.horizon.horizon_api.application.mapper.TicketMapper;
import br.com.horizon.horizon_api.application.usecase.GenerateTicketsUseCase;
import br.com.horizon.horizon_api.application.usecase.GetReservationTicketsUseCase;
import br.com.horizon.horizon_api.domain.model.Passenger;
import br.com.horizon.horizon_api.domain.model.Reservation;
import br.com.horizon.horizon_api.domain.model.Ticket;
import br.com.horizon.horizon_api.domain.port.ReservationPersistencePort;
import br.com.horizon.horizon_api.domain.port.TicketPersistencePort;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.ReservationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketService implements GenerateTicketsUseCase, GetReservationTicketsUseCase {

    private final TicketPersistencePort ticketPort;
    private final ReservationPersistencePort reservationPort;
    private final TicketMapper ticketMapper;

    @Override
    @Transactional
    public List<Ticket> execute(Reservation reservation) {
        if (reservation.getStatus() != ReservationStatus.CONFIRMADA) {
            throw new IllegalStateException("Tickets can only be generated for CONFIRMADA reservations.");
        }

        List<Passenger> passengers = reservation.getPassengers();
        if (passengers == null || passengers.isEmpty()) {
            log.warn("Reservation {} has no passengers to generate tickets for.", reservation.getId());
            return List.of();
        }

        List<Ticket> generatedTickets = new ArrayList<>();
        int year = LocalDate.now().getYear();

        for (Passenger passenger : passengers) {
            // Idempotency: verify if ticket already exists for this passenger
            if (passenger.getId() != null && ticketPort.existsByPassengerId(passenger.getId())) {
                log.info("Ticket already exists for passenger ID: {}. Skipping duplication.", passenger.getId());
                ticketPort.findByPassengerId(passenger.getId()).ifPresent(generatedTickets::add);
                continue;
            }

            int randomSuffix = ThreadLocalRandom.current().nextInt(1000, 9999);
            String ticketNumber = String.format("HZ-%d-R%d-P%d-%04d", year, reservation.getId(), passenger.getId(), randomSuffix);

            Ticket ticket = new Ticket();
            ticket.setReservationId(reservation.getId());
            ticket.setPassengerId(passenger.getId());
            ticket.setPassengerName(passenger.getName());
            ticket.setTicketNumber(ticketNumber);
            ticket.setGeneratedAt(OffsetDateTime.now());
            // Structure prepared for future PDF rendering / download
            ticket.setPdfUrl(String.format("/tickets/%s/download", ticketNumber));
            ticket.setEmailSentAt(null); // Reserved for future email sending

            Ticket saved = ticketPort.save(ticket);
            generatedTickets.add(saved);
            log.info("Generated ticket {} for passenger {} (Reservation {})", ticketNumber, passenger.getName(), reservation.getId());
        }

        return generatedTickets;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> execute(Long reservationId, Long userId) {
        // Ensure reservation exists and belongs to the authenticated user
        Reservation reservation = reservationPort.findByIdAndUserId(reservationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found or access denied."));

        List<Ticket> tickets = ticketPort.findByReservationId(reservation.getId());
        return tickets.stream()
                .map(ticketMapper::toResponse)
                .collect(Collectors.toList());
    }
}
