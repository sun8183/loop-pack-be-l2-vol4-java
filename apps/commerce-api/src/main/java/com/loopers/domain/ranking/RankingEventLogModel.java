package com.loopers.domain.ranking;

import com.loopers.domain.BaseEntity;
import com.loopers.support.Guard;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ranking_event_log", uniqueConstraints = {
        @UniqueConstraint(name = "uq_ranking_event_log_event_key", columnNames = {"event_key"})
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RankingEventLogModel extends BaseEntity {

    @Column(name = "event_key", nullable = false, length = 200)
    private String eventKey;

    public RankingEventLogModel(String eventKey) {
        Guard.notBlank(eventKey, "이벤트 키는 필수입니다.");
        this.eventKey = eventKey;
    }

    public String getEventKey() {
        return eventKey;
    }
}
