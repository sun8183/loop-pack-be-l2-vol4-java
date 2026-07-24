package com.loopers.batch.job.ranking.monthly;

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
import java.time.LocalDate;

@ConditionalOnProperty(name = "spring.batch.job.name", havingValue = MonthlyProductRankJobConfig.JOB_NAME)
@RequiredArgsConstructor
@Configuration
public class MonthlyProductRankJobConfig {
    public static final String JOB_NAME = "monthlyProductRankJob";
    private static final String STEP_NAME = "monthlyMetricAggregationStep";
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
    public Job monthlyProductRankJob(Step monthlyMetricAggregationStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(monthlyMetricAggregationStep)
                .listener(jobListener)
                .build();
    }

    @JobScope
    @Bean(STEP_NAME)
    public Step monthlyMetricAggregationStep(
            JdbcCursorItemReader<ProductMetricRow> monthlyMetricReader,
            RankAggregationWriter monthlyRankWriter) {
        return new StepBuilder(STEP_NAME, jobRepository)
                .<ProductMetricRow, ProductScore>chunk(CHUNK_SIZE, transactionManager)
                .reader(monthlyMetricReader)
                .processor(productScoreItemProcessor)
                .writer(monthlyRankWriter)
                .listener((StepExecutionListener) monthlyRankWriter)
                .listener(stepMonitorListener)
                .listener(chunkListener)
                .build();
    }

    @StepScope
    @Bean
    public JdbcCursorItemReader<ProductMetricRow> monthlyMetricReader(
            @Value("#{jobParameters['requestDate']}") String requestDate) {
        LocalDate baseDate = LocalDate.parse(requestDate);
        LocalDate periodStart = baseDate.withDayOfMonth(1);
        return ProductMetricReaderFactory.create(dataSource, periodStart, baseDate);
    }

    @StepScope
    @Bean
    public RankAggregationWriter monthlyRankWriter(
            @Value("#{jobParameters['requestDate']}") String requestDate) {
        LocalDate baseDate = LocalDate.parse(requestDate);
        LocalDate periodStart = baseDate.withDayOfMonth(1);
        return new RankAggregationWriter(mvRankRepository, MvRankTable.MONTHLY, TOP_N, periodStart, baseDate);
    }
}
