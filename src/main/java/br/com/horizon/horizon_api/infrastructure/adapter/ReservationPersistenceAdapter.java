package br.com.horizon.horizon_api.infrastructure.adapter;

import br.com.horizon.horizon_api.domain.model.Passenger;
import br.com.horizon.horizon_api.domain.model.Payment;
import br.com.horizon.horizon_api.domain.model.Reservation;
import br.com.horizon.horizon_api.domain.port.ReservationPersistencePort;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.DestinationEntity;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.FlightEntity;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.PassengerEntity;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.PaymentEntity;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.ReservationEntity;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.UserEntity;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.ReservationStatus;
import br.com.horizon.horizon_api.infrastructure.persistence.repository.PassengerRepository;
import br.com.horizon.horizon_api.infrastructure.persistence.repository.PaymentRepository;
import br.com.horizon.horizon_api.infrastructure.persistence.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ReservationPersistenceAdapter implements ReservationPersistencePort {

    private final ReservationRepository reservationRepository;
    private final PassengerRepository passengerRepository;
    private final PaymentRepository paymentRepository;

    @Override
    @Transactional
    public Reservation save(Reservation domain) {
        boolean isNew = (domain.getId() == null);

        ReservationEntity entity = new ReservationEntity();
        if (!isNew) {
            entity = reservationRepository.findById(domain.getId()).orElse(new ReservationEntity());
        }

        UserEntity user = new UserEntity();
        user.setId(domain.getUserId());
        entity.setUser(user);

        DestinationEntity dest = new DestinationEntity();
        dest.setId(domain.getDestinationId());
        entity.setDestination(dest);

        FlightEntity outFlight = new FlightEntity();
        outFlight.setId(domain.getOutboundFlightId());
        entity.setOutboundFlight(outFlight);

        if (domain.getReturnFlightId() != null) {
            FlightEntity retFlight = new FlightEntity();
            retFlight.setId(domain.getReturnFlightId());
            entity.setReturnFlight(retFlight);
        }

        entity.setTripType(domain.getTripType());
        entity.setDepartureDate(domain.getDepartureDate());
        entity.setReturnDate(domain.getReturnDate());
        entity.setNumberOfPassengers(domain.getNumberOfPassengers());
        entity.setTotalAmount(domain.getTotalAmount());
        entity.setStatus(domain.getStatus());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setPaymentDeadline(domain.getPaymentDeadline());
        entity.setCancelledAt(domain.getCancelledAt());

        ReservationEntity saved = reservationRepository.save(entity);
        domain.setId(saved.getId());

        // Only persist passengers on creation
        if (isNew && domain.getPassengers() != null) {
            for (Passenger p : domain.getPassengers()) {
                PassengerEntity pe = new PassengerEntity();
                pe.setReservation(saved);
                pe.setName(p.getName());
                pe.setAge(p.getAge());
                passengerRepository.save(pe);
                p.setId(pe.getId());
            }
        }

        if (domain.getPayment() != null) {
            PaymentEntity payE;
            if (domain.getPayment().getId() != null) {
                payE = paymentRepository.findById(domain.getPayment().getId()).orElse(new PaymentEntity());
            } else {
                payE = paymentRepository.findByReservationId(saved.getId()).orElse(new PaymentEntity());
            }
            payE.setReservation(saved);
            payE.setPaymentMethod(domain.getPayment().getPaymentMethod());
            payE.setStatus(domain.getPayment().getStatus());
            payE.setAmount(domain.getPayment().getAmount());
            payE.setCreatedAt(domain.getPayment().getCreatedAt());
            payE.setPaidAt(domain.getPayment().getPaidAt());
            paymentRepository.save(payE);
            domain.getPayment().setId(payE.getId());
        }

        return domain;
    }

    @Override
    public Optional<Reservation> findById(Long id) {
        return reservationRepository.findById(id).map(e -> toDomain(e, true));
    }

    @Override
    public Optional<Reservation> findByIdAndUserId(Long id, Long userId) {
        return reservationRepository.findByIdAndUserId(id, userId).map(e -> toDomain(e, true));
    }

    @Override
    public List<Reservation> findByUserId(Long userId) {
        return reservationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(e -> toDomain(e, false)).collect(Collectors.toList());
    }

    @Override
    public List<Reservation> findExpiredPendingReservations(OffsetDateTime now) {
        return reservationRepository.findByStatusAndPaymentDeadlineBefore(ReservationStatus.PENDENTE, now)
                .stream().map(e -> toDomain(e, true)).collect(Collectors.toList());
    }

    @Override
    public List<Reservation> findConfirmedReservationsPastEndDate(java.time.LocalDate currentDate, java.time.LocalTime currentTime) {
        return reservationRepository.findConfirmedReservationsPastEndDate(currentDate, currentTime).stream()
                .map(e -> toDomain(e, true))
                .collect(Collectors.toList());
    }

    private Reservation toDomain(ReservationEntity entity, boolean fetchPayment) {
        Reservation r = new Reservation();
        r.setId(entity.getId());
        r.setUserId(entity.getUser().getId());
        r.setDestinationId(entity.getDestination().getId());
        r.setOutboundFlightId(entity.getOutboundFlight().getId());
        if (entity.getReturnFlight() != null) {
            r.setReturnFlightId(entity.getReturnFlight().getId());
        }
        r.setTripType(entity.getTripType());
        r.setDepartureDate(entity.getDepartureDate());
        r.setReturnDate(entity.getReturnDate());
        r.setNumberOfPassengers(entity.getNumberOfPassengers());
        r.setTotalAmount(entity.getTotalAmount());
        r.setStatus(entity.getStatus());
        r.setCreatedAt(entity.getCreatedAt());
        r.setPaymentDeadline(entity.getPaymentDeadline());
        r.setCancelledAt(entity.getCancelledAt());

        if (fetchPayment) {
            paymentRepository.findByReservationId(entity.getId()).ifPresent(pay -> {
                Payment p = new Payment();
                p.setId(pay.getId());
                p.setReservationId(entity.getId());
                p.setStatus(pay.getStatus());
                p.setPaymentMethod(pay.getPaymentMethod());
                p.setAmount(pay.getAmount());
                p.setCreatedAt(pay.getCreatedAt());
                p.setPaidAt(pay.getPaidAt());
                r.setPayment(p);
            });
        }

        List<PassengerEntity> passengerEntities = passengerRepository.findByReservationId(entity.getId());
        if (passengerEntities != null && !passengerEntities.isEmpty()) {
            List<Passenger> passList = passengerEntities.stream().map(pe -> {
                Passenger p = new Passenger();
                p.setId(pe.getId());
                p.setReservationId(entity.getId());
                p.setName(pe.getName());
                p.setAge(pe.getAge());
                return p;
            }).collect(Collectors.toList());
            r.setPassengers(passList);
        }

        return r;
    }
}
