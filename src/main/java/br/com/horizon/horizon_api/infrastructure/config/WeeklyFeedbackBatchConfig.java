package br.com.horizon.horizon_api.infrastructure.config;

import br.com.horizon.horizon_api.domain.port.EmailPort;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.FeedbackEntity;
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
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepContribution;
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

    @Value("${FEEDBACK_REPORT_EMAIL:admin@horizon.com}")
    private String feedbackReportEmail;

    @Bean
    public Tasklet weeklyFeedbackTasklet() {
        return (contribution, chunkContext) -> {
            OffsetDateTime sevenDaysAgo = OffsetDateTime.now().minusDays(7);
            List<FeedbackEntity> recentFeedbacks = feedbackRepository.findByAnsweredAtAfter(sevenDaysAgo);

            if (recentFeedbacks.isEmpty()) {
                emailPort.sendEmail(feedbackReportEmail, "Horizon - Relatorio Semanal NPS/CSAT", "Nenhum feedback recebido nos ultimos 7 dias.");
                return RepeatStatus.FINISHED;
            }

            long platformTotal = 0;
            long platformPromoters = 0;
            long platformDetractors = 0;
            double platformScoreSum = 0;

            long tripTotal = 0;
            long tripPromoters = 0;
            long tripDetractors = 0;
            double tripScoreSum = 0;

            for (FeedbackEntity f : recentFeedbacks) {
                if (f.getScore() == null) continue;
                int score = f.getScore();
                
                if (f.getTargetType() == TargetType.PLATFORM) {
                    platformTotal++;
                    platformScoreSum += score;
                    if (score >= 9) platformPromoters++;
                    else if (score <= 6) platformDetractors++;
                } else if (f.getTargetType() == TargetType.TRIP) {
                    tripTotal++;
                    tripScoreSum += score;
                    if (score >= 9) tripPromoters++;
                    else if (score <= 6) tripDetractors++;
                }
            }

            StringBuilder report = new StringBuilder();
            report.append("Horizon - Relatorio Semanal NPS/CSAT\n\n");
            
            report.append("--- PLATAFORMA ---\n");
            if (platformTotal > 0) {
                double platformNps = ((double) (platformPromoters - platformDetractors) / platformTotal) * 100;
                double platformCsat = platformScoreSum / platformTotal;
                report.append("Total de feedbacks: ").append(platformTotal).append("\n");
                report.append(String.format("NPS: %.2f%%\n", platformNps));
                report.append(String.format("CSAT: %.2f\n\n", platformCsat));
            } else {
                report.append("Nenhum feedback de plataforma.\n\n");
            }

            report.append("--- VIAGENS ---\n");
            if (tripTotal > 0) {
                double tripNps = ((double) (tripPromoters - tripDetractors) / tripTotal) * 100;
                double tripCsat = tripScoreSum / tripTotal;
                report.append("Total de feedbacks: ").append(tripTotal).append("\n");
                report.append(String.format("NPS: %.2f%%\n", tripNps));
                report.append(String.format("CSAT: %.2f\n", tripCsat));
            } else {
                report.append("Nenhum feedback de viagem.\n");
            }

            emailPort.sendEmail(feedbackReportEmail, "Horizon - Relatorio Semanal NPS/CSAT", report.toString());
            log.info("[Spring Batch] Relatorio Semanal NPS/CSAT enviado para {}", feedbackReportEmail);

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

    // Run every Sunday at midnight (0 0 0 * * SUN)
    @Scheduled(cron = "0 0 0 * * SUN")
    public void launchWeeklyFeedbackJob() {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("runTime", System.currentTimeMillis())
                    .toJobParameters();
            Job job = applicationContext.getBean("weeklyFeedbackJob", Job.class);
            jobLauncher.run(job, params);
        } catch (Exception e) {
            log.error("[Spring Batch] Error launching weekly feedback job", e);
        }
    }
}
