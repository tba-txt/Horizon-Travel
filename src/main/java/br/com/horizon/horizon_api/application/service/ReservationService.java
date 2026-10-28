package br.com.horizon.horizon_api.application.service;

import br.com.horizon.horizon_api.application.dto.request.PassengerRequest;
import br.com.horizon.horizon_api.application.dto.request.ReservationRequest;
import br.com.horizon.horizon_api.application.dto.response.FlightResponse;
import br.com.horizon.horizon_api.application.dto.response.MyTripsResponse;
import br.com.horizon.horizon_api.application.dto.response.PassengerResponse;
import br.com.horizon.horizon_api.application.dto.response.ReservationResponse;
import br.com.horizon.horizon_api.application.mapper.TicketMapper;
import br.com.horizon.horizon_api.application.usecase.CreateReservationUseCase;
import br.com.horizon.horizon_api.application.usecase.GetReservationByIdUseCase;
import br.com.horizon.horizon_api.application.usecase.ListUserReservationsUseCase;
import br.com.horizon.horizon_api.domain.model.Flight;
import br.com.horizon.horizon_api.domain.model.Passenger;
import br.com.horizon.horizon_api.domain.model.Payment;
import br.com.horizon.horizon_api.domain.model.Reservation;
import br.com.horizon.horizon_api.domain.model.Ticket;
import br.com.horizon.horizon_api.domain.port.DestinationPersistencePort;
import br.com.horizon.horizon_api.domain.port.FlightPersistencePort;
import br.com.horizon.horizon_api.domain.port.ReservationPersistencePort;
import br.com.horizon.horizon_api.domain.port.TicketPersistencePort;
import br.com.horizon.horizon_api.domain.port.UserPersistencePort;
import br.com.horizon.horizon_api.domain.model.User;
import java.time.temporal.ChronoUnit;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.PaymentMethod;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.PaymentStatus;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.ReservationStatus;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.TripType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class ReservationService implements CreateReservationUseCase, GetReservationByIdUseCase, ListUserReservationsUseCase {

    private final ReservationPersistencePort reservationPort;
    private final FlightPersistencePort flightPort;
    private final DestinationPersistencePort destinationPort;
    private final TicketPersistencePort ticketPort;
    private final UserPersistencePort userPort;
    private final TicketMapper ticketMapper;

    @Override
    @Transactional
    public ReservationResponse execute(Long userId, ReservationRequest request) {
        User buyer = userPort.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
        if (!buyer.isAdult()) {
            throw new IllegalArgumentException("O comprador deve ter pelo menos 18 anos.");
        }

        if (request.getPassengers() == null || request.getPassengers().isEmpty() || request.getPassengers().size() > 5) {
            throw new IllegalArgumentException("A reserva deve conter entre 1 e 5 passageiros.");
        }

        List<Passenger> passengers = new ArrayList<>();
        for (PassengerRequest pReq : request.getPassengers()) {
            if (pReq.getName() == null || pReq.getName().trim().isEmpty()) {
                throw new IllegalArgumentException("O nome do passageiro não pode ser vazio.");
            }
            if (pReq.getAge() == null || pReq.getAge() < 1 || pReq.getAge() > 99) {
                throw new IllegalArgumentException("A idade do passageiro deve estar entre 1 e 99 anos.");
            }
            Passenger p = new Passenger();
            p.setName(pReq.getName());
            p.setAge(pReq.getAge());
            passengers.add(p);
        }

        int numPassengers = passengers.size();

        Flight outbound = flightPort.getFlightById(request.getOutboundFlightId())
                .orElseThrow(() -> new IllegalArgumentException("Voo de ida não encontrado."));

        if (outbound.getActive() == null || !outbound.getActive()) {
            throw new IllegalArgumentException("O voo de ida selecionado não está ativo.");
        }

        if (!outbound.getDestinationId().equals(request.getDestinationId())) {
            throw new IllegalArgumentException("O voo de ida não corresponde ao destino da reserva.");
        }

        LocalDate today = LocalDate.now();
        LocalDate minDeparture = today.plusDays(3);
        LocalDate maxDeparture = today.plusMonths(24);

        if (outbound.getFlightDate().isBefore(minDeparture)) {
            throw new IllegalArgumentException("A data de partida deve ter antecedência mínima de 3 dias.");
        }
        if (outbound.getFlightDate().isAfter(maxDeparture)) {
            throw new IllegalArgumentException("A data de partida não pode exceder 24 meses.");
        }

        BigDecimal totalPerPerson = getFlightPrice(outbound, request.getOutboundSeatClass());
        Flight returnFlight = null;

        if (request.getTripType() == TripType.ROUND_TRIP) {
            if (request.getReturnFlightId() == null) {
                throw new IllegalArgumentException("Voo de volta é obrigatório para viagens de ida e volta.");
            }
            returnFlight = flightPort.getFlightById(request.getReturnFlightId())
                    .orElseThrow(() -> new IllegalArgumentException("Voo de volta não encontrado."));

            if (returnFlight.getActive() == null || !returnFlight.getActive()) {
                throw new IllegalArgumentException("O voo de volta selecionado não está ativo.");
            }

            if (returnFlight.getFlightDate().isBefore(outbound.getFlightDate())) {
                throw new IllegalArgumentException("A data de volta não pode ser anterior à data de partida.");
            }

            // Validação de intervalo mínimo entre ida e volta:
            // Para países na América do Sul: pelo menos 1 dia de estadia (pode ir em um dia e voltar no seguinte).
            // Para destinos internacionais fora da América do Sul (Europa, Ásia, América do Norte, etc.):
            // devido ao longo tempo de voo e fuso horário, a volta deve ser pelo menos 3 dias após a ida.
            var destinationOpt = destinationPort.findById(request.getDestinationId());
            String country = destinationOpt.map(d -> d.getCountry() != null ? d.getCountry().toLowerCase() : "").orElse("");
            boolean isSouthAmerica = country.contains("brasil") || country.contains("argentina") || 
                                     country.contains("chile") || country.contains("peru") || 
                                     country.contains("colômbia") || country.contains("colombia") || 
                                     country.contains("equador") || country.contains("uruguai") || 
                                     country.contains("paraguai") || country.contains("bolívia") || 
                                     country.contains("bolivia");

            long daysBetween = ChronoUnit.DAYS.between(outbound.getFlightDate(), returnFlight.getFlightDate());
            if (isSouthAmerica) {
                if (daysBetween < 1) {
                    throw new IllegalArgumentException("Para voos na América do Sul, a data de volta deve ser no mínimo 1 dia após a data de partida.");
                }
            } else {
                if (daysBetween < 3) {
                    throw new IllegalArgumentException("Para voos internacionais fora da América do Sul, a data de volta deve ser no mínimo 3 dias após a partida devido ao tempo de voo e conexão.");
                }
            }

            totalPerPerson = totalPerPerson.add(getFlightPrice(returnFlight, request.getReturnSeatClass()));
        }

        boolean outReserved = flightPort.reserveSeats(outbound.getId(), numPassengers);
        if (!outReserved) {
            throw new IllegalStateException("Não há assentos suficientes disponíveis no voo de ida.");
        }

        if (returnFlight != null) {
            boolean retReserved = flightPort.reserveSeats(returnFlight.getId(), numPassengers);
            if (!retReserved) {
                throw new IllegalStateException("Não há assentos suficientes disponíveis no voo de volta.");
            }
        }

        BigDecimal totalAmount = totalPerPerson.multiply(BigDecimal.valueOf(numPassengers));

        Reservation reservation = new Reservation();
        reservation.setUserId(userId);
        reservation.setDestinationId(request.getDestinationId());
        reservation.setOutboundFlightId(outbound.getId());
        if (returnFlight != null) {
            reservation.setReturnFlightId(returnFlight.getId());
            reservation.setReturnDate(returnFlight.getFlightDate());
        }
        reservation.setTripType(request.getTripType());
        reservation.setDepartureDate(outbound.getFlightDate());
        reservation.setNumberOfPassengers(numPassengers);
        reservation.setTotalAmount(totalAmount);
        reservation.setStatus(ReservationStatus.PENDENTE);

        OffsetDateTime now = OffsetDateTime.now();
        reservation.setCreatedAt(now);
        reservation.setPaymentDeadline(now.plusHours(24));
        reservation.setPassengers(passengers);

        Payment payment = new Payment();
        payment.setAmount(totalAmount);
        payment.setStatus(PaymentStatus.PENDENTE);
        payment.setPaymentMethod(PaymentMethod.PIX);
        payment.setCreatedAt(now);
        reservation.setPayment(payment);

        Reservation saved = reservationPort.save(reservation);
        log.info("Created new reservation ID: {} for User ID: {}", saved.getId(), userId);

        ReservationResponse response = toDetailedResponse(saved);
        if (response.getOutboundFlight() != null && request.getOutboundSeatClass() != null) {
            response.getOutboundFlight().setSeatClass(request.getOutboundSeatClass());
        }
        if (response.getReturnFlight() != null && request.getReturnSeatClass() != null) {
            response.getReturnFlight().setSeatClass(request.getReturnSeatClass());
        }
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public ReservationResponse execute(Long id, Long userId) {
        Reservation res = reservationPort.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Reserva não encontrada."));
        return toDetailedResponse(res);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationResponse> execute(Long userId) {
        return reservationPort.findByUserId(userId).stream()
                .map(this::toDetailedResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public MyTripsResponse executeMyTrips(Long userId) {
        List<Reservation> userReservations = reservationPort.findByUserId(userId);
        MyTripsResponse myTrips = new MyTripsResponse();

        if (userReservations == null || userReservations.isEmpty()) {
            return myTrips;
        }

        // Batch fetch tickets for reservations that may have them (CONFIRMADA / CONCLUIDA)
        List<Long> paidReservationIds = userReservations.stream()
                .filter(r -> r.getStatus() == ReservationStatus.CONFIRMADA || r.getStatus() == ReservationStatus.CONCLUIDA)
                .map(Reservation::getId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());

        java.util.Map<Long, List<Ticket>> ticketsByReservation = new java.util.HashMap<>();
        if (ticketPort != null && !paidReservationIds.isEmpty()) {
            List<Ticket> allTickets = ticketPort.findByReservationIdIn(paidReservationIds);
            if (allTickets != null) {
                ticketsByReservation = allTickets.stream()
                        .filter(t -> t.getReservationId() != null)
                        .collect(Collectors.groupingBy(Ticket::getReservationId));
            }
        }

        for (Reservation res : userReservations) {
            if (ticketsByReservation.containsKey(res.getId())) {
                res.setTickets(ticketsByReservation.get(res.getId()));
            }
            ReservationResponse resp = toDetailedResponse(res);
            if (res.getStatus() == ReservationStatus.PENDENTE) {
                myTrips.getPending().add(resp);
            } else if (res.getStatus() == ReservationStatus.CONFIRMADA) {
                myTrips.getConfirmed().add(resp);
            } else if (res.getStatus() == ReservationStatus.CONCLUIDA) {
                myTrips.getFinished().add(resp);
            } else if (res.getStatus() == ReservationStatus.CANCELADA) {
                myTrips.getCancelled().add(resp);
            }
        }

        return myTrips;
    }

    private ReservationResponse toDetailedResponse(Reservation res) {
        ReservationResponse resp = new ReservationResponse();
        resp.setReservationId(res.getId());
        resp.setDestinationId(res.getDestinationId());

        // Destination name: use pre-fetched name or fallback to query
        if (res.getDestinationName() != null && !res.getDestinationName().isBlank()) {
            resp.setDestinationName(res.getDestinationName());
        } else if (destinationPort != null && res.getDestinationId() != null) {
            destinationPort.findById(res.getDestinationId()).ifPresent(d -> resp.setDestinationName(d.getName()));
        }

        resp.setTripType(res.getTripType());
        resp.setDepartureDate(res.getDepartureDate());
        resp.setReturnDate(res.getReturnDate());
        resp.setTotalAmount(res.getTotalAmount());
        resp.setStatus(res.getStatus());
        resp.setPaymentDeadline(res.getPaymentDeadline());
        resp.setCreatedAt(res.getCreatedAt());
        resp.setCancelledAt(res.getCancelledAt());

        // Outbound Flight details: use pre-fetched flight or fallback to query
        Flight outboundFlight = res.getOutboundFlight();
        if (outboundFlight == null && res.getOutboundFlightId() != null && flightPort != null) {
            outboundFlight = flightPort.getFlightById(res.getOutboundFlightId()).orElse(null);
        }
        if (outboundFlight != null) {
            FlightResponse fr = mapFlight(outboundFlight);
            if (res.getNumberOfPassengers() != null && res.getNumberOfPassengers() > 0 && res.getTotalAmount() != null) {
                if (res.getTripType() == TripType.ONE_WAY) {
                    BigDecimal perPerson = res.getTotalAmount().divide(BigDecimal.valueOf(res.getNumberOfPassengers()), 2, java.math.RoundingMode.HALF_UP);
                    fr.setSeatClass(deduceSeatClass(outboundFlight, perPerson));
                }
            }
            resp.setOutboundFlight(fr);
        }

        // Return Flight details: use pre-fetched flight or fallback to query
        Flight returnFlight = res.getReturnFlight();
        if (returnFlight == null && res.getReturnFlightId() != null && flightPort != null) {
            returnFlight = flightPort.getFlightById(res.getReturnFlightId()).orElse(null);
        }
        if (returnFlight != null) {
            resp.setReturnFlight(mapFlight(returnFlight));
        }

        // Passengers
        if (res.getPassengers() != null) {
            resp.setPassengers(res.getPassengers().stream().map(p -> {
                PassengerResponse pr = new PassengerResponse();
                pr.setId(p.getId());
                pr.setName(p.getName());
                pr.setAge(p.getAge());
                return pr;
            }).collect(Collectors.toList()));
        }

        // Tickets (when they exist)
        if (res.getStatus() == ReservationStatus.PENDENTE) {
            resp.setTickets(null); // tickets are only generated after payment
        } else if (res.getTickets() != null) {
            // Already pre-fetched in batch!
            resp.setTickets(res.getTickets().stream().map(ticketMapper::toResponse).collect(Collectors.toList()));
        } else if (ticketPort != null && res.getId() != null) {
            List<Ticket> tickets = ticketPort.findByReservationId(res.getId());
            if (tickets != null && !tickets.isEmpty()) {
                resp.setTickets(tickets.stream().map(ticketMapper::toResponse).collect(Collectors.toList()));
            }
        }

        return resp;
    }

    private FlightResponse mapFlight(Flight f) {
        FlightResponse fr = new FlightResponse();
        fr.setId(f.getId());
        fr.setDestinationId(f.getDestinationId());
        fr.setOriginAirportCode(f.getOriginAirportCode());
        fr.setDestinationAirportCode(f.getDestinationAirportCode());
        fr.setFlightDate(f.getFlightDate());
        fr.setDepartureTime(f.getDepartureTime());
        fr.setArrivalTime(f.getArrivalTime());
        fr.setFlightNumber(f.getFlightNumber());
        fr.setPricePerPerson(f.getPricePerPerson());
        fr.setPriceExecutive(f.getPriceExecutive());
        fr.setPricePremium(f.getPricePremium());
        return fr;
    }
private BigDecimal getFlightPrice(Flight flight, String seatClass) {
        if (flight == null) return BigDecimal.ZERO;
        if (seatClass != null) {
            String normalized = seatClass.trim().toUpperCase();
            if ("EXECUTIVE".equals(normalized) && flight.getPriceExecutive() != null) {
                return flight.getPriceExecutive();
            }
            if ("PREMIUM".equals(normalized) && flight.getPricePremium() != null) {
                return flight.getPricePremium();
            }
        }
        return flight.getPricePerPerson() != null ? flight.getPricePerPerson() : BigDecimal.ZERO;
    }

    private String deduceSeatClass(Flight f, BigDecimal legPrice) {
        if (f == null || legPrice == null) return "BASIC";
        if (f.getPricePremium() != null && f.getPricePremium().compareTo(legPrice) == 0) {
            return "PREMIUM";
        }
        if (f.getPriceExecutive() != null && f.getPriceExecutive().compareTo(legPrice) == 0) {
            return "EXECUTIVE";
        }
        return "BASIC";
    }
}
