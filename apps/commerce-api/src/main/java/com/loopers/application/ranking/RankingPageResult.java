package com.loopers.application.ranking;

import java.util.List;

public record RankingPageResult(List<RankingInfo> content, long totalElements) {
}
