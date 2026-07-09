package com.loopers.domain.outbox;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.outbox.enums.OutboxEventStatus;
import com.loopers.support.Guard;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "outbox_events")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class OutboxEvent extends BaseEntity {

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String topic;

    @Column(nullable = false)
    private String messageKey;

    @Lob
    @Column(nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxEventStatus status;

    public OutboxEvent(String eventType, String topic, String messageKey, String payload) {
        Guard.notBlank(eventType, "이벤트 타입은 필수입니다.");
        Guard.notBlank(topic, "토픽명은 필수입니다.");
        Guard.notBlank(messageKey, "메시지 키는 필수입니다.");
        Guard.notBlank(payload, "페이로드는 필수입니다.");
        this.eventType = eventType;
        this.topic = topic;
        this.messageKey = messageKey;
        this.payload = payload;
        this.status = OutboxEventStatus.PENDING;
    }

    public void markSent() {
        this.status = OutboxEventStatus.SENT;
    }
}
