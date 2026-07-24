package com.loopers.batch.job.ranking;

import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RequiredArgsConstructor
public class RankAggregationWriter implements ItemWriter<ProductScore>, StepExecutionListener {

    private final MvRankRepository mvRankRepository;
    private final MvRankTable table;
    private final int topN;
    private final LocalDate periodStart;
    private final LocalDate periodEnd;

    private final Map<Long, Double> scoreByProduct = new ConcurrentHashMap<>();

    @Override
    public void write(@Nonnull Chunk<? extends ProductScore> chunk) {
        for (ProductScore item : chunk) {
            scoreByProduct.merge(item.productId(), item.score(), Double::sum);
        }
    }

    @Override
    public ExitStatus afterStep(@Nonnull StepExecution stepExecution) {
        if (stepExecution.getStatus() != BatchStatus.COMPLETED) {
            return stepExecution.getExitStatus();
        }

        List<Map.Entry<Long, Double>> topEntries = scoreByProduct.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(topN)
                .toList();

        List<RankedProduct> ranked = new ArrayList<>(topEntries.size());
        long rank = 1;
        for (Map.Entry<Long, Double> entry : topEntries) {
            ranked.add(new RankedProduct(entry.getKey(), rank++, entry.getValue()));
        }

        mvRankRepository.replaceAll(table, periodStart, periodEnd, ranked);
        log.info("MV 랭킹 반영 완료 [table={}, periodStart={}, periodEnd={}, count={}]",
                table.tableName(), periodStart, periodEnd, ranked.size());

        return stepExecution.getExitStatus();
    }
}
