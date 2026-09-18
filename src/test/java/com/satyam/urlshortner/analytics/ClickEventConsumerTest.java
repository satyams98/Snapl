package com.satyam.urlshortner.analytics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClickEventConsumerTest {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().findAndAddModules().build();

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

        consumer.onClickEvent(JSON_MAPPER.writeValueAsString(event));

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

        consumer.onClickEvent(JSON_MAPPER.writeValueAsString(event));
    }

    @Test
    void onClickEventDoesNotThrowOnMalformedPayload() {
        consumer.onClickEvent("not valid json");

        verify(repository, never()).save(any());
    }
}
