package br.com.horizon.horizon_api.infrastructure.config;

import br.com.horizon.horizon_api.application.usecase.CancelReservationUseCase;
import br.com.horizon.horizon_api.application.usecase.FinalizeReservationUseCase;
import br.com.horizon.horizon_api.domain.model.Reservation;
import br.com.horizon.horizon_api.domain.port.ReservationPersistencePort;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.ReservationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Iterator;
import java.util.List;

/**
 * Spring Batch Configuration for Automatic Cancellation and Finalization of Reservations.
 *
 * Implements the canonical Spring Batch pattern:
 * - Job: cancelExpiredReservationsJob
 *   - Step 1: cancelExpiredReservationsStep (chunk-oriented)
 *   - Step 2: finalizeReservationsStep (chunk-oriented)
 * - ItemReader / ItemProcessor / ItemWriter for each step
 * - JobLauncher triggered via @Scheduled cron every 5 minutes.
 */
@Configuration
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class BatchConfig {

    private final ReservationPersistencePort reservationPort;
    private final CancelReservationUseCase cancelReservationUseCase;
    private final FinalizeReservationUseCase finalizeReservationUseCase;
    private final JobLauncher jobLauncher;
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final ApplicationContext applicationContext;

    @Bean
    public ItemReader<Reservation> expiredReservationReader() {
        return new ItemReader<Reservation>() {
            private Iterator<Reservation> iterator;

            @Override
            public Reservation read() {
                if (iterator == null) {
                    List<Reservation> expired = reservationPort.findExpiredPendingReservations(OffsetDateTime.now());
                    iterator = expired.iterator();
                }
                if (iterator.hasNext()) {
                    return iterator.next();
                }
                iterator = null; // Reset for next job execution
                return null;    // Signals end of step
            }
        };
    }

    @Bean
    public ItemProcessor<Reservation, Reservation> expiredReservationProcessor() {
        return reservation -> {
            // Idempotency filter: ensure only PENDENTE reservations are processed
            if (reservation.getStatus() != ReservationStatus.PENDENTE) {
                return null; // Returning null filters out the item
            }
            log.info("[Spring Batch] Processing expiration for Reservation ID: {}", reservation.getId());
            return reservation;
        };
    }

    @Bean
    public ItemWriter<Reservation> expiredReservationWriter() {
        return chunk -> {
            for (Reservation reservation : chunk) {
                try {
                    // CancelReservationUseCase is transactional and idempotent
                    cancelReservationUseCase.execute(reservation.getId(), null);
                    log.info("[Spring Batch] Successfully cancelled expired Reservation ID: {}", reservation.getId());
                } catch (Exception e) {
                    log.error("[Spring Batch] Failed to cancel Reservation ID: {}", reservation.getId(), e);
                }
            }
        };
    }

    @Bean
    public Step cancelExpiredReservationsStep(ItemReader<Reservation> expiredReservationReader,
                                             ItemProcessor<Reservation, Reservation> expiredReservationProcessor,
                                             ItemWriter<Reservation> expiredReservationWriter) {
        return new StepBuilder("cancelExpiredReservationsStep", jobRepository)
                .<Reservation, Reservation>chunk(10)
                .transactionManager(transactionManager)
                .reader(expiredReservationReader)
                .processor(expiredReservationProcessor)
                .writer(expiredReservationWriter)
                .build();
    }

    @Bean
    public ItemReader<Reservation> finalizedReservationReader() {
        return new ItemReader<Reservation>() {
            private Iterator<Reservation> iterator;

            @Override
            public Reservation read() {
                if (iterator == null) {
                    List<Reservation> finished = reservationPort.findConfirmedReservationsPastEndDate(
                            LocalDate.now(), LocalTime.now());
                    iterator = finished.iterator();
                }
                if (iterator.hasNext()) {
                    return iterator.next();
                }
                iterator = null;
                return null;
            }
        };
    }

    @Bean
    public ItemProcessor<Reservation, Reservation> finalizedReservationProcessor() {
        return reservation -> {
            if (reservation.getStatus() != ReservationStatus.CONFIRMADA) {
                return null;
            }
            log.info("[Spring Batch] Processing finalization for Reservation ID: {}", reservation.getId());
            return reservation;
        };
    }

    @Bean
    public ItemWriter<Reservation> finalizedReservationWriter() {
        return chunk -> {
            for (Reservation reservation : chunk) {
                try {
                    finalizeReservationUseCase.execute(reservation.getId());
                } catch (Exception e) {
                    log.error("[Spring Batch] Failed to finalize Reservation ID: {}", reservation.getId(), e);
                }
            }
        };
    }

    @Bean
    public Step finalizeReservationsStep(ItemReader<Reservation> finalizedReservationReader,
                                         ItemProcessor<Reservation, Reservation> finalizedReservationProcessor,
                                         ItemWriter<Reservation> finalizedReservationWriter) {
        return new StepBuilder("finalizeReservationsStep", jobRepository)
                .<Reservation, Reservation>chunk(10)
                .transactionManager(transactionManager)
                .reader(finalizedReservationReader)
                .processor(finalizedReservationProcessor)
                .writer(finalizedReservationWriter)
                .build();
    }

    @Bean
    public Job cancelExpiredReservationsJob(Step cancelExpiredReservationsStep, Step finalizeReservationsStep) {
        return new JobBuilder("cancelExpiredReservationsJob", jobRepository)
                .start(cancelExpiredReservationsStep)
                .next(finalizeReservationsStep)
                .build();
    }

    @Scheduled(cron = "0 */5 * * * *")
    public void launchExpiredReservationsJob() {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("runTime", System.currentTimeMillis())
                    .toJobParameters();
            Job job = applicationContext.getBean("cancelExpiredReservationsJob", Job.class);
            jobLauncher.run(job, params);
        } catch (Exception e) {
            log.error("[Spring Batch] Error launching batch job", e);
        }
    }
}
