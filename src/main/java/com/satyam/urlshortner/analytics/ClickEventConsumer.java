package com.satyam.urlshortner.analytics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.time.ZoneOffset;

@Component
public class ClickEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ClickEventConsumer.class);

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().findAndAddModules().build();

    private final ClickEventRepository repository;
    private final ClickDailyStatsRepository dailyStatsRepository;

    public ClickEventConsumer(ClickEventRepository repository, ClickDailyStatsRepository dailyStatsRepository) {
        this.repository = repository;
        this.dailyStatsRepository = dailyStatsRepository;
    }

    @KafkaListener(topics = "${app.kafka.click-events-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void onClickEvent(String payload) {
        try {
            ClickEvent event = JSON_MAPPER.readValue(payload, ClickEvent.class);
            UserAgentParser.ParsedUserAgent parsed = UserAgentParser.parse(event.userAgent());
            ClickEventEntity entity = new ClickEventEntity(null, event.shortCode(), event.clickedAt(),
                    event.ipAddress(), event.userAgent(), event.referrer(),
                    parsed.deviceType(), parsed.browser(), parsed.os(), null, null);
            // Listener container threads are isolated from the WebFlux event loop,
            // so blocking here does not affect redirect latency.
            repository.save(entity)
                    .then(dailyStatsRepository.increment(event.shortCode(), event.clickedAt().atZone(ZoneOffset.UTC).toLocalDate()))
                    .block();
        } catch (Exception ex) {
            log.error("Failed to persist click event: {}", ex.getMessage());
        }
    }
}

