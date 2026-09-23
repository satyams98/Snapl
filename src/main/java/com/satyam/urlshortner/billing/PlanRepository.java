package com.satyam.urlshortner.billing;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PlanRepository extends ReactiveCrudRepository<Plan, Long> {
    Mono<Plan> findByCode(String code);

    Flux<Plan> findAllByOrderByPriceCentsAsc();
}
