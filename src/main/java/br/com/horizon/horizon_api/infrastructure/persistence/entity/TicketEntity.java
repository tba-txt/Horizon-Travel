package br.com.horizon.horizon_api.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "tickets")
@Getter
@Setter
public class TicketEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private ReservationEntity reservation;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "passenger_id", unique = true, nullable = false)
    private PassengerEntity passenger;

    @Column(name = "ticket_number", length = 50, unique = true)
    private String ticketNumber;

    @Column(name = "pdf_url", columnDefinition = "TEXT")
    private String pdfUrl;

    @Column(name = "generated_at")
    private OffsetDateTime generatedAt;

    @Column(name = "email_sent_at")
    private OffsetDateTime emailSentAt;
}
