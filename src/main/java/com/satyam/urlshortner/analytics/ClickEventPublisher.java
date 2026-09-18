package com.satyam.urlshortner.analytics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class ClickEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ClickEventPublisher.class);

    // spring-kafka's built-in JsonSerializer targets Jackson 2, which conflicts with Boot 4.1's
    // default Jackson 3 on the classpath, so events are serialized to plain JSON strings instead.
    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().findAndAddModules().build();

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaTopicsProperties topics;

    public ClickEventPublisher(KafkaTemplate<String, String> kafkaTemplate, KafkaTopicsProperties topics) {
        this.kafkaTemplate = kafkaTemplate;
        this.topics = topics;
    }

    // Fire-and-forget: click tracking must never slow down or fail a redirect.
    public void publish(ClickEvent event) {
        String payload = JSON_MAPPER.writeValueAsString(event);
        kafkaTemplate.send(topics.clickEventsTopic(), event.shortCode(), payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.warn("Failed to publish click event for {}: {}", event.shortCode(), ex.getMessage());
                    }
                });
    }
}
