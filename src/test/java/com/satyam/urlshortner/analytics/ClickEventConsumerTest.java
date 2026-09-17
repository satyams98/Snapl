package com.satyam.urlshortner.analytics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClickEventConsumerTest {

    @Mock
    private ClickEventRepository repository;

    private ClickEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new ClickEventConsumer(repository);
    }

    @Test
    void onClickEventPersistsEntityMappedFromMessage() {
        Instant now = Instant.now();
        ClickEvent event = new ClickEvent("abc123", now, "127.0.0.1", "curl/8.0", "https://referrer.example.com");
        when(repository.save(any(ClickEventEntity.class))).thenReturn(Mono.just(new ClickEventEntity()));

        consumer.onClickEvent(event);

        ArgumentCaptor<ClickEventEntity> captor = ArgumentCaptor.forClass(ClickEventEntity.class);
        verify(repository).save(captor.capture());
        ClickEventEntity saved = captor.getValue();
        assertEquals("abc123", saved.getShortCode());
        assertEquals(now, saved.getClickedAt());
        assertEquals("127.0.0.1", saved.getIpAddress());
        assertEquals("curl/8.0", saved.getUserAgent());
        assertEquals("https://referrer.example.com", saved.getReferrer());
    }

    @Test
    void onClickEventDoesNotThrowWhenPersistenceFails() {
        ClickEvent event = new ClickEvent("abc123", Instant.now(), "127.0.0.1", "curl/8.0", null);
        when(repository.save(any(ClickEventEntity.class))).thenReturn(Mono.error(new RuntimeException("db down")));

        consumer.onClickEvent(event);
    }
}
