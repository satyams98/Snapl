package com.satyam.urlshortner.analytics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class ClickEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ClickEventPublisher.class);

    private final KafkaTemplate<String, ClickEvent> kafkaTemplate;
    private final KafkaTopicsProperties topics;

    public ClickEventPublisher(KafkaTemplate<String, ClickEvent> kafkaTemplate, KafkaTopicsProperties topics) {
        this.kafkaTemplate = kafkaTemplate;
        this.topics = topics;
    }

    // Fire-and-forget: click tracking must never slow down or fail a redirect.
    public void publish(ClickEvent event) {
        kafkaTemplate.send(topics.clickEventsTopic(), event.shortCode(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.warn("Failed to publish click event for {}: {}", event.shortCode(), ex.getMessage());
                    }
                });
    }
}
