package br.com.horizon.horizon_api.infrastructure.config;

import br.com.horizon.horizon_api.domain.port.EmailPort;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.FeedbackEntity;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.FeedbackType;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.TargetType;
import br.com.horizon.horizon_api.infrastructure.persistence.repository.FeedbackRepository;
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
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class WeeklyFeedbackBatchConfig {

    private final JobLauncher jobLauncher;
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final FeedbackRepository feedbackRepository;
    private final EmailPort emailPort;
    private final ApplicationContext applicationContext;

    @Value("${FEEDBACK_REPORT_EMAIL:gabrieloliveiratba@hotmail.com}")
    private String feedbackReportEmail;

    @Bean
    public Tasklet weeklyFeedbackTasklet() {
        return (contribution, chunkContext) -> {
            OffsetDateTime threeDaysAgo = OffsetDateTime.now().minusDays(3);
            List<FeedbackEntity> recentFeedbacks = feedbackRepository.findRecentFeedbacks(threeDaysAgo);

            String subject = "Horizon - Síntese de Avaliações (NPS e CSAT - Últimos 3 Dias)";

            String nowStr = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

            if (recentFeedbacks == null || recentFeedbacks.isEmpty()) {
                log.info("[Spring Batch NPS/CSAT] Nenhuma avaliação registrada nos últimos 3 dias. Enviando síntese vazia com layout Horizon para {}.", feedbackReportEmail);
                String emptyReportHtml = br.com.horizon.horizon_api.application.service.EmailTemplateService.buildWeeklyFeedbackReportEmail(
                        "Período: Últimos 3 dias (Emitido em " + nowStr + ")",
                        0,
                        0, 0.0, 0, 0, 0,
                        0, 0.0, 0.0,
                        0, 0.0, 0, 0.0,
                        0, 0.0, 0, 0.0,
                        java.util.List.of()
                );
                emailPort.sendEmail(feedbackReportEmail, subject, emptyReportHtml);
                return RepeatStatus.FINISHED;
            }

            long npsTotal = 0, npsPromoters = 0, npsPassives = 0, npsDetractors = 0;
            long csatTotal = 0, csatSatisfied = 0;
            double csatSum = 0;

            long platNpsTotal = 0, platNpsPromoters = 0, platNpsDetractors = 0;
            long platCsatTotal = 0;
            double platCsatSum = 0;

            long tripNpsTotal = 0, tripNpsPromoters = 0, tripNpsDetractors = 0;
            long tripCsatTotal = 0;
            double tripCsatSum = 0;

            List<String> commentsList = new java.util.ArrayList<>();

            for (FeedbackEntity f : recentFeedbacks) {
                if (f.getScore() == null) continue;
                int score = f.getScore();

                if (f.getFeedbackType() == FeedbackType.NPS) {
                    npsTotal++;
                    if (score >= 9) npsPromoters++;
                    else if (score <= 6) npsDetractors++;
                    else npsPassives++;

                    if (f.getTargetType() == TargetType.PLATFORM) {
                        platNpsTotal++;
                        if (score >= 9) platNpsPromoters++;
                        else if (score <= 6) platNpsDetractors++;
                    } else if (f.getTargetType() == TargetType.TRIP) {
                        tripNpsTotal++;
                        if (score >= 9) tripNpsPromoters++;
                        else if (score <= 6) tripNpsDetractors++;
                    }
                } else if (f.getFeedbackType() == FeedbackType.CSAT) {
                    csatTotal++;
                    csatSum += score;
                    if (score >= 4) csatSatisfied++;

                    if (f.getTargetType() == TargetType.PLATFORM) {
                        platCsatTotal++;
                        platCsatSum += score;
                    } else if (f.getTargetType() == TargetType.TRIP) {
                        tripCsatTotal++;
                        tripCsatSum += score;
                    }
                }

                if (f.getComment() != null && !f.getComment().isBlank()) {
                    String canal = f.getTargetType() == TargetType.TRIP ? "Viagem" : "Plataforma";
                    String tipo = f.getFeedbackType() == FeedbackType.NPS ? "NPS " + score + "/10" : "CSAT " + score + "/5";
                    commentsList.add(String.format("[%s - %s]: \"%s\"", canal, tipo, f.getComment().trim()));
                }
            }

            long totalCount = npsTotal + csatTotal;
            if (totalCount == 0) {
                String emptyReportHtml = br.com.horizon.horizon_api.application.service.EmailTemplateService.buildWeeklyFeedbackReportEmail(
                        "Período: Últimos 3 dias (Emitido em " + nowStr + ")",
                        0,
                        0, 0.0, 0, 0, 0,
                        0, 0.0, 0.0,
                        0, 0.0, 0, 0.0,
                        0, 0.0, 0, 0.0,
                        java.util.List.of()
                );
                emailPort.sendEmail(feedbackReportEmail, subject, emptyReportHtml);
                return RepeatStatus.FINISHED;
            }

            double npsGeral = npsTotal > 0 ? ((npsPromoters - npsDetractors) * 100.0) / npsTotal : 0.0;
            double csatMediaGeral = csatTotal > 0 ? csatSum / csatTotal : 0.0;
            double csatPctGeral = csatTotal > 0 ? (csatSatisfied * 100.0) / csatTotal : 0.0;

            double platNpsScore = platNpsTotal > 0 ? ((platNpsPromoters - platNpsDetractors) * 100.0) / platNpsTotal : 0.0;
            double platCsatMedia = platCsatTotal > 0 ? platCsatSum / platCsatTotal : 0.0;
            double tripNpsScore = tripNpsTotal > 0 ? ((tripNpsPromoters - tripNpsDetractors) * 100.0) / tripNpsTotal : 0.0;
            double tripCsatMedia = tripCsatTotal > 0 ? tripCsatSum / tripCsatTotal : 0.0;

            String htmlReport = br.com.horizon.horizon_api.application.service.EmailTemplateService.buildWeeklyFeedbackReportEmail(
                    "Período: Últimos 3 dias (Emitido em " + nowStr + ")",
                    totalCount,
                    npsTotal, npsGeral, npsPromoters, npsPassives, npsDetractors,
                    csatTotal, csatMediaGeral, csatPctGeral,
                    platCsatTotal, platCsatMedia, platNpsTotal, platNpsScore,
                    tripCsatTotal, tripCsatMedia, tripNpsTotal, tripNpsScore,
                    commentsList
            );

            emailPort.sendEmail(feedbackReportEmail, subject, htmlReport);
            log.info("[Spring Batch NPS/CSAT] Síntese de 3 dias enviada com sucesso para {}", feedbackReportEmail);

            return RepeatStatus.FINISHED;
        };
    }

    @Bean
    public Step weeklyFeedbackStep(Tasklet weeklyFeedbackTasklet) {
        return new StepBuilder("weeklyFeedbackStep", jobRepository)
                .tasklet(weeklyFeedbackTasklet)
                .transactionManager(transactionManager)
                .build();
    }

    @Bean
    public Job weeklyFeedbackJob(Step weeklyFeedbackStep) {
        return new JobBuilder("weeklyFeedbackJob", jobRepository)
                .start(weeklyFeedbackStep)
                .build();
    }

    // Executa a cada 3 dias às 08:00 AM (0 0 8 */3 * *)
    @Scheduled(cron = "${batch.feedback.cron:0 0 8 */3 * *}")
    public void launchWeeklyFeedbackJob() {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("runTime", System.currentTimeMillis())
                    .toJobParameters();
            Job job = applicationContext.getBean("weeklyFeedbackJob", Job.class);
            jobLauncher.run(job, params);
        } catch (Exception e) {
            log.error("[Spring Batch] Erro ao executar batch de síntese de avaliações (3 dias)", e);
        }
    }
}
