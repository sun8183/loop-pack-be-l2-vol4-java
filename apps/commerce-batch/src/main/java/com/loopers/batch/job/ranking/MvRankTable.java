package com.loopers.batch.job.ranking;

public enum MvRankTable {
    WEEKLY("mv_product_rank_weekly"),
    MONTHLY("mv_product_rank_monthly");

    private final String tableName;

    MvRankTable(String tableName) {
        this.tableName = tableName;
    }

    public String tableName() {
        return tableName;
    }
}
