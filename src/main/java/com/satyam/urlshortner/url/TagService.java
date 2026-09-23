package com.satyam.urlshortner.url;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;

    public Flux<Tag> listForOrg(Long orgId) {
        return tagRepository.findByOrgIdOrderByNameAsc(orgId);
    }

    // Tags are created on first use rather than requiring a separate "create tag" step.
    public Flux<Tag> resolveOrCreate(Long orgId, List<String> names) {
        List<String> distinctNames = names.stream()
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .distinct()
                .toList();

        return Flux.fromIterable(distinctNames)
                .concatMap(name -> tagRepository.findByOrgIdAndName(orgId, name)
                        .switchIfEmpty(Mono.defer(() -> tagRepository.save(new Tag(null, orgId, name, Instant.now())))));
    }
}
