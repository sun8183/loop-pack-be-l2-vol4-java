package com.loopers.batch.job.ranking.weekly;

import com.loopers.batch.job.ranking.MvRankRepository;
import com.loopers.batch.job.ranking.MvRankTable;
import com.loopers.batch.job.ranking.ProductMetricReaderFactory;
import com.loopers.batch.job.ranking.ProductMetricRow;
import com.loopers.batch.job.ranking.ProductScore;
import com.loopers.batch.job.ranking.ProductScoreItemProcessor;
import com.loopers.batch.job.ranking.RankAggregationWriter;
import com.loopers.batch.listener.ChunkListener;
import com.loopers.batch.listener.JobListener;
import com.loopers.batch.listener.StepMonitorListener;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.time.DayOfWeek;
import java.time.LocalDate;

@ConditionalOnProperty(name = "spring.batch.job.name", havingValue = WeeklyProductRankJobConfig.JOB_NAME)
@RequiredArgsConstructor
@Configuration
public class WeeklyProductRankJobConfig {
    public static final String JOB_NAME = "weeklyProductRankJob";
    private static final String STEP_NAME = "weeklyMetricAggregationStep";
    private static final int CHUNK_SIZE = 500;
    private static final int TOP_N = 100;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final DataSource dataSource;
    private final MvRankRepository mvRankRepository;
    private final ProductScoreItemProcessor productScoreItemProcessor;
    private final JobListener jobListener;
    private final StepMonitorListener stepMonitorListener;
    private final ChunkListener chunkListener;

    @Bean(JOB_NAME)
    public Job weeklyProductRankJob(Step weeklyMetricAggregationStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(weeklyMetricAggregationStep)
                .listener(jobListener)
                .build();
    }

    @JobScope
    @Bean(STEP_NAME)
    public Step weeklyMetricAggregationStep(
            JdbcCursorItemReader<ProductMetricRow> weeklyMetricReader,
            RankAggregationWriter weeklyRankWriter) {
        return new StepBuilder(STEP_NAME, jobRepository)
                .<ProductMetricRow, ProductScore>chunk(CHUNK_SIZE, transactionManager)
                .reader(weeklyMetricReader)
                .processor(productScoreItemProcessor)
                .writer(weeklyRankWriter)
                .listener((StepExecutionListener) weeklyRankWriter)
                .listener(stepMonitorListener)
                .listener(chunkListener)
                .build();
    }

    @StepScope
    @Bean
    public JdbcCursorItemReader<ProductMetricRow> weeklyMetricReader(
            @Value("#{jobParameters['requestDate']}") String requestDate) {
        LocalDate baseDate = LocalDate.parse(requestDate);
        LocalDate periodStart = baseDate.with(DayOfWeek.MONDAY);
        return ProductMetricReaderFactory.create(dataSource, periodStart, baseDate);
    }

    @StepScope
    @Bean
    public RankAggregationWriter weeklyRankWriter(
            @Value("#{jobParameters['requestDate']}") String requestDate) {
        LocalDate baseDate = LocalDate.parse(requestDate);
        LocalDate periodStart = baseDate.with(DayOfWeek.MONDAY);
        return new RankAggregationWriter(mvRankRepository, MvRankTable.WEEKLY, TOP_N, periodStart, baseDate);
    }
}
