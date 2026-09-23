package com.satyam.urlshortner.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;

@Table("click_daily_stats")
@Data
@AllArgsConstructor
public class ClickDailyStatsEntity implements Persistable<Long> {

    @Id
    private Long id;

    @Column("short_code")
    private String shortCode;

    @Column("day")
    private LocalDate day;

    @Column("click_count")
    private long clickCount;

    @Override
    public boolean isNew() {
        return id == null;
    }
}
