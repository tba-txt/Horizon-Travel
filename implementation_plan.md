# Post-Checkout & Cancellation Flow Implementation Plan

## Proposed Changes

### 1. Domain Models & Enums
- Ensure ReservationStatus and PaymentStatus include necessary states (FINALIZADA or CANCELADA, etc., as appropriate).
- No new entities needed. We'll use existing ones.

### 2. Application Ports and Adapters
- Add logic in ReservationPersistenceAdapter to properly update Payment and Reservation statuses.
- Update ReservationPersistencePort: Optional<Reservation> findById(Long id), Reservation save(Reservation r).

### 3. Application Use Cases
- ConfirmPaymentUseCase: POST /payments/{reservationId}/confirm (accepts PIX or CARD). Updates statuses.
- CancelReservationUseCase: POST /reservations/{id}/cancel. Updates statuses, releases seats.

### 4. Application Services
- PaymentService (implements ConfirmPaymentUseCase):
  - Validates payment_deadline >= now.
  - Ensures reservation is PENDENTE and payment is PENDENTE.
  - Updates payment to PAGO, sets paidAt.
  - Updates reservation to CONFIRMADA.
- CancellationService (implements CancelReservationUseCase):
  - Validates reservation state (can't cancel if already CANCELADA/FINALIZADA).
  - Updates reservation to CANCELADA, sets cancelledAt.
  - If it was PENDENTE, also updates Payment to CANCELADO.
  - Returns seats to light_availability by doing an atomic update: UPDATE FlightAvailabilityEntity SET availableSeats = availableSeats + :qty WHERE ... AND availableSeats + :qty <= totalSeats.
  
### 5. Interface Controllers
- PaymentController: POST /payments/{reservationId}/confirm
- ReservationController: Add POST /reservations/{id}/cancel

### 6. Spring Batch
- Job: cancelExpiredReservationsJob
- Step: cancelExpiredStep
- Reader: Fetch reservations where status = PENDENTE and paymentDeadline < now().
- Processor: Mark reservation as CANCELADA, payment as CANCELADO, release seats.
- Writer: Save updated reservations and payments. (Could just do ItemProcessor + ItemWriter).
- Config: Schedule every 5 minutes (@Scheduled(cron = "0 */5 * * * *") - wait, the user said "Implementar o Batch REAL do Horizon usando Spring Batch. NÃO substituir por @Scheduled. Periodicidade: A cada 5 minutos." This means we use a Spring Scheduler to LAUNCH the Spring Batch Job).

### 7. Maven configuration
- Update pom.xml to set lombok version to 1.18.38.

## Verification Plan
- Unit tests for CancellationService, PaymentService, and Batch logic.
- Run mvnw clean package to confirm the Java 25 / Lombok 1.18.38 fix works.
