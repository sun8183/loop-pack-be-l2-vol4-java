package com.loopers.batch.job.ranking;

import org.springframework.batch.item.database.JdbcCursorItemReader;

import javax.sql.DataSource;
import java.time.LocalDate;

public final class ProductMetricReaderFactory {

    private ProductMetricReaderFactory() {
    }

    public static JdbcCursorItemReader<ProductMetricRow> create(DataSource dataSource, LocalDate periodStart, LocalDate periodEnd) {
        JdbcCursorItemReader<ProductMetricRow> reader = new JdbcCursorItemReader<>();
        reader.setDataSource(dataSource);
        reader.setSql("SELECT product_id, order_count, like_count FROM product_metrics WHERE base_date BETWEEN ? AND ? ORDER BY product_id");
        reader.setPreparedStatementSetter(ps -> {
            ps.setObject(1, periodStart);
            ps.setObject(2, periodEnd);
        });
        reader.setRowMapper((rs, rowNum) -> new ProductMetricRow(
                rs.getLong("product_id"), rs.getLong("order_count"), rs.getLong("like_count")));
        return reader;
    }
}
