package com.satyam.urlshortner.url;

import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

// The url_tags join table has a composite key with no natural single-column id, so it's managed
// with hand-written SQL via DatabaseClient rather than forced into a ReactiveCrudRepository.
@Repository
@RequiredArgsConstructor
public class TagAssignmentDao {

    private final DatabaseClient databaseClient;

    public Flux<String> findTagNamesForUrl(Long urlId) {
        return databaseClient.sql("SELECT t.name AS name FROM tags t JOIN url_tags ut ON ut.tag_id = t.id WHERE ut.url_id = :urlId ORDER BY t.name")
                .bind("urlId", urlId)
                .map(row -> row.get("name", String.class))
                .all();
    }

    public Mono<Void> replaceTags(Long urlId, List<Long> tagIds) {
        return clearTags(urlId).then(addTags(urlId, tagIds));
    }

    public Mono<Void> addTags(Long urlId, List<Long> tagIds) {
        if (tagIds.isEmpty()) {
            return Mono.empty();
        }
        return Flux.fromIterable(tagIds)
                .concatMap(tagId -> databaseClient.sql(
                                "INSERT INTO url_tags (url_id, tag_id) VALUES (:urlId, :tagId) ON CONFLICT DO NOTHING")
                        .bind("urlId", urlId)
                        .bind("tagId", tagId)
                        .fetch().rowsUpdated())
                .then();
    }

    private Mono<Void> clearTags(Long urlId) {
        return databaseClient.sql("DELETE FROM url_tags WHERE url_id = :urlId")
                .bind("urlId", urlId)
                .fetch().rowsUpdated().then();
    }
}
