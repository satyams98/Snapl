package com.satyam.urlshortner.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Table("click_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClickEventEntity {

    @Id
    private Long id;

    @Column("short_code")
    private String shortCode;

    @Column("clicked_at")
    private Instant clickedAt;

    @Column("ip_address")
    private String ipAddress;

    @Column("user_agent")
    private String userAgent;

    private String referrer;

    @Column("device_type")
    private String deviceType;

    @Column("browser")
    private String browser;

    @Column("os")
    private String os;

    @Column("country")
    private String country;

    @Column("city")
    private String city;
}
