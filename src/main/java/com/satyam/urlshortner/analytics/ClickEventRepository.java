package com.satyam.urlshortner.analytics;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface ClickEventRepository extends ReactiveCrudRepository<ClickEventEntity, Long> {
}
