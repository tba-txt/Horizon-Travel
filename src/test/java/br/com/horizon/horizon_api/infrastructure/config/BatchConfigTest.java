package br.com.horizon.horizon_api.infrastructure.config;

import br.com.horizon.horizon_api.application.usecase.CancelReservationUseCase;
import br.com.horizon.horizon_api.application.usecase.FinalizeReservationUseCase;
import br.com.horizon.horizon_api.domain.model.Reservation;
import br.com.horizon.horizon_api.domain.port.ReservationPersistencePort;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.ReservationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BatchConfigTest {

    @Mock
    private ReservationPersistencePort reservationPort;

    @Mock
    private CancelReservationUseCase cancelReservationUseCase;

    @Mock
    private FinalizeReservationUseCase finalizeReservationUseCase;

    @Mock
    private br.com.horizon.horizon_api.application.usecase.TriggerFeedbackUseCase triggerFeedbackUseCase;

    @Mock
    private JobLauncher jobLauncher;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private ApplicationContext applicationContext;

    private BatchConfig batchConfig;

    @BeforeEach
    void setUp() {
        batchConfig = new BatchConfig(
                reservationPort,
                cancelReservationUseCase,
                finalizeReservationUseCase,
                triggerFeedbackUseCase,
                jobLauncher,
                jobRepository,
                transactionManager,
                applicationContext
        );
    }

    @Test
    void shouldReadExpiredPendingReservations() throws Exception {
        Reservation res = new Reservation();
        res.setId(10L);
        res.setStatus(ReservationStatus.PENDENTE);
        res.setPaymentDeadline(OffsetDateTime.now().minusHours(25));

        when(reservationPort.findExpiredPendingReservations(any())).thenReturn(List.of(res));

        ItemReader<Reservation> reader = batchConfig.expiredReservationReader();
        Reservation readItem = reader.read();

        assertNotNull(readItem);
        assertEquals(10L, readItem.getId());

        // Subsequent read should return null indicating end of stream
        assertNull(reader.read());
    }

    @Test
    void shouldFilterNonPendingReservationsInProcessor() throws Exception {
        ItemProcessor<Reservation, Reservation> processor = batchConfig.expiredReservationProcessor();

        Reservation pending = new Reservation();
        pending.setId(1L);
        pending.setStatus(ReservationStatus.PENDENTE);

        Reservation confirmed = new Reservation();
        confirmed.setId(2L);
        confirmed.setStatus(ReservationStatus.CONFIRMADA);

        Reservation cancelled = new Reservation();
        cancelled.setId(3L);
        cancelled.setStatus(ReservationStatus.CANCELADA);

        assertEquals(pending, processor.process(pending), "PENDENTE should pass through");
        assertNull(processor.process(confirmed), "CONFIRMADA should be filtered out");
        assertNull(processor.process(cancelled), "CANCELADA should be filtered out");
    }

    @Test
    void shouldCancelExpiredReservationsInWriter() throws Exception {
        ItemWriter<Reservation> writer = batchConfig.expiredReservationWriter();

        Reservation res1 = new Reservation();
        res1.setId(100L);
        res1.setStatus(ReservationStatus.PENDENTE);

        Reservation res2 = new Reservation();
        res2.setId(200L);
        res2.setStatus(ReservationStatus.PENDENTE);

        Chunk<Reservation> chunk = new Chunk<>(List.of(res1, res2));
        writer.write(chunk);

        verify(cancelReservationUseCase).execute(100L, null);
        verify(cancelReservationUseCase).execute(200L, null);
    }
}
