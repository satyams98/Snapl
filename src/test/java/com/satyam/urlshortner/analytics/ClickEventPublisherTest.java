package com.satyam.urlshortner.analytics;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClickEventPublisherTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private ClickEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new ClickEventPublisher(kafkaTemplate, new KafkaTopicsProperties("url.click-events"));
    }

    @Test
    void publishSendsEventKeyedByShortCodeAsJson() {
        ClickEvent event = new ClickEvent("abc123", Instant.now(), "127.0.0.1", "curl/8.0", null);
        SendResult<String, String> sendResult = new SendResult<>(null, mock(RecordMetadata.class));
        when(kafkaTemplate.send(eq("url.click-events"), eq("abc123"), anyString()))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        publisher.publish(event);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("url.click-events"), eq("abc123"), payloadCaptor.capture());
        assertTrue(payloadCaptor.getValue().contains("abc123"));
    }

    @Test
    void publishSwallowsKafkaFailuresWithoutThrowing() {
        ClickEvent event = new ClickEvent("abc123", Instant.now(), "127.0.0.1", "curl/8.0", null);
        CompletableFuture<SendResult<String, String>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("broker unavailable"));
        when(kafkaTemplate.send(eq("url.click-events"), eq("abc123"), anyString())).thenReturn(failed);

        publisher.publish(event);
    }
}
