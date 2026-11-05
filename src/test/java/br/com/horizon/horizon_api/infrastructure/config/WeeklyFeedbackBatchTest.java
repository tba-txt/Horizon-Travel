package br.com.horizon.horizon_api.infrastructure.config;

import br.com.horizon.horizon_api.domain.port.EmailPort;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.FeedbackEntity;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.FeedbackType;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.TargetType;
import br.com.horizon.horizon_api.infrastructure.persistence.repository.FeedbackRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeeklyFeedbackBatchTest {

    @Mock
    private FeedbackRepository feedbackRepository;

    @Mock
    private EmailPort emailPort;

    @Mock
    private StepContribution stepContribution;

    @Mock
    private ChunkContext chunkContext;

    private WeeklyFeedbackBatchConfig batchConfig;
    private final String testEmail = "gestao@horizon.com";

    @BeforeEach
    void setUp() {
        batchConfig = new WeeklyFeedbackBatchConfig(
                null, null, null,
                feedbackRepository, emailPort, null
        );
        ReflectionTestUtils.setField(batchConfig, "feedbackReportEmail", testEmail);
    }

    @Test
    @DisplayName("Quando não há avaliações recentes: gera síntese vazia e envia e-mail com layout Horizon")
    void shouldSendEmptyReportWhenNoFeedbacksExist() throws Exception {
        when(feedbackRepository.findRecentFeedbacks(any(OffsetDateTime.class))).thenReturn(Collections.emptyList());

        Tasklet tasklet = batchConfig.weeklyFeedbackTasklet();
        RepeatStatus status = tasklet.execute(stepContribution, chunkContext);

        assertEquals(RepeatStatus.FINISHED, status);
        verify(emailPort, times(1)).sendEmail(
                eq(testEmail),
                contains("Síntese de Avaliações"),
                anyString()
        );
    }

    @Test
    @DisplayName("Quando há avaliações: consolida NPS e CSAT (plataforma e viagem) e despacha e-mail estruturado")
    void shouldConsolidateMetricsAndSendReport() throws Exception {
        // NPS Promotor Plataforma (10)
        FeedbackEntity f1 = new FeedbackEntity();
        f1.setFeedbackType(FeedbackType.NPS);
        f1.setTargetType(TargetType.PLATFORM);
        f1.setScore(10);
        f1.setComment("Interface incrível e intuitiva!");

        // NPS Detrator Viagem (5)
        FeedbackEntity f2 = new FeedbackEntity();
        f2.setFeedbackType(FeedbackType.NPS);
        f2.setTargetType(TargetType.TRIP);
        f2.setScore(5);
        f2.setComment("Atraso de voo da companhia parceira.");

        // CSAT Satisfeito Plataforma (5)
        FeedbackEntity f3 = new FeedbackEntity();
        f3.setFeedbackType(FeedbackType.CSAT);
        f3.setTargetType(TargetType.PLATFORM);
        f3.setScore(5);

        // CSAT Neutro Viagem (3)
        FeedbackEntity f4 = new FeedbackEntity();
        f4.setFeedbackType(FeedbackType.CSAT);
        f4.setTargetType(TargetType.TRIP);
        f4.setScore(3);

        when(feedbackRepository.findRecentFeedbacks(any(OffsetDateTime.class))).thenReturn(List.of(f1, f2, f3, f4));

        Tasklet tasklet = batchConfig.weeklyFeedbackTasklet();
        RepeatStatus status = tasklet.execute(stepContribution, chunkContext);

        assertEquals(RepeatStatus.FINISHED, status);
        verify(emailPort, times(1)).sendEmail(
                eq(testEmail),
                contains("Síntese de Avaliações"),
                argThat(html -> html.contains("Interface incrível") && html.contains("Atraso de voo"))
        );
    }
}
