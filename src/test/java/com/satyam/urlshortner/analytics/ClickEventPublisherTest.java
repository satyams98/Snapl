package com.satyam.urlshortner.analytics;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClickEventPublisherTest {

    @Mock
    private KafkaTemplate<String, ClickEvent> kafkaTemplate;

    private ClickEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new ClickEventPublisher(kafkaTemplate, new KafkaTopicsProperties("url.click-events"));
    }

    @Test
    void publishSendsEventKeyedByShortCode() {
        ClickEvent event = new ClickEvent("abc123", Instant.now(), "127.0.0.1", "curl/8.0", null);
        SendResult<String, ClickEvent> sendResult = new SendResult<>(null, mock(RecordMetadata.class));
        when(kafkaTemplate.send(eq("url.click-events"), eq("abc123"), eq(event)))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        publisher.publish(event);

        verify(kafkaTemplate).send("url.click-events", "abc123", event);
    }

    @Test
    void publishSwallowsKafkaFailuresWithoutThrowing() {
        ClickEvent event = new ClickEvent("abc123", Instant.now(), "127.0.0.1", "curl/8.0", null);
        CompletableFuture<SendResult<String, ClickEvent>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("broker unavailable"));
        when(kafkaTemplate.send(eq("url.click-events"), eq("abc123"), eq(event))).thenReturn(failed);

        publisher.publish(event);
    }
}
