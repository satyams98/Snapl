package com.satyam.urlshortner.url;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UrlQueryService {

    private final R2dbcEntityTemplate entityTemplate;
    private final UrlRepository urlRepository;
    private final FolderRepository folderRepository;
    private final TagRepository tagRepository;
    private final TagAssignmentDao tagAssignmentDao;
    private final TagService tagService;
    private final UrlService urlService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.base-url}")
    private String baseUrl;

    public Mono<UrlListResponse> list(Long orgId, String search, Long folderId, String status, String tag, int page, int size) {
        Criteria criteria = Criteria.where("org_id").is(orgId);
        if (search != null && !search.isBlank()) {
            String pattern = "%" + search.trim() + "%";
            criteria = criteria.and(Criteria.where("short_code").like(pattern).or(Criteria.where("long_url").like(pattern)));
        }
        if (folderId != null) {
            criteria = criteria.and("folder_id").is(folderId);
        }
        if ("active".equalsIgnoreCase(status)) {
            criteria = criteria.and("disabled_at").isNull();
        } else if ("disabled".equalsIgnoreCase(status)) {
            criteria = criteria.and("disabled_at").isNotNull();
        }

        Query sortedQuery = Query.query(criteria).sort(Sort.by(Sort.Direction.DESC, "created_at"));

        if (tag == null || tag.isBlank()) {
            Mono<Long> countMono = entityTemplate.count(Query.query(criteria), UrlEntity.class);
            Query pageQuery = sortedQuery.limit(size).offset((long) page * size);
            return entityTemplate.select(pageQuery, UrlEntity.class)
                    .collectList()
                    .flatMap(entities -> toResponses(entities).collectList())
                    .zipWith(countMono)
                    .map(t -> new UrlListResponse(t.getT1(), t.getT2(), page, size));
        }

        // Tag filtering runs in-memory (no SQL join), so every org/search/folder/status match is pulled first.
        return entityTemplate.select(sortedQuery, UrlEntity.class)
                .concatMap(entity -> tagAssignmentDao.findTagNamesForUrl(entity.getId()).collectList()
                        .map(tags -> tags.stream().anyMatch(t -> t.equalsIgnoreCase(tag)) ? entity : null))
                .filter(Objects::nonNull)
                .collectList()
                .flatMap(filtered -> {
                    long total = filtered.size();
                    List<UrlEntity> pageSlice = filtered.stream().skip((long) page * size).limit(size).toList();
                    return toResponses(pageSlice).collectList().map(items -> new UrlListResponse(items, total, page, size));
                });
    }

    public Mono<UrlSummaryResponse> getDetail(Long orgId, String shortCode) {
        return urlRepository.findByShortCodeAndOrgId(shortCode, orgId)
                .switchIfEmpty(Mono.error(new ShortUrlNotFoundException(shortCode)))
                .flatMap(this::toSummary);
    }

    public Mono<UrlSummaryResponse> update(Long orgId, String shortCode, UpdateUrlRequest request) {
        return urlRepository.findByShortCodeAndOrgId(shortCode, orgId)
                .switchIfEmpty(Mono.error(new ShortUrlNotFoundException(shortCode)))
                .flatMap(existing -> applyUpdate(orgId, shortCode, existing, request))
                .then(assignTagsIfPresent(orgId, shortCode, request.tags()))
                .then(urlService.evictCache(shortCode))
                .then(getDetail(orgId, shortCode));
    }

    private Mono<Integer> applyUpdate(Long orgId, String shortCode, UrlEntity existing, UpdateUrlRequest request) {
        String longUrl = request.longUrl() != null ? request.longUrl() : existing.getLongUrl();
        String longUrlHash = request.longUrl() != null ? UrlHasher.sha256Hex(request.longUrl()) : existing.getLongUrlHash();
        var expiresAt = request.expiresAt() != null ? request.expiresAt() : existing.getExpiresAt();
        var folderId = request.folderId() != null ? request.folderId() : existing.getFolderId();
        var startsAt = request.startsAt() != null ? request.startsAt() : existing.getStartsAt();
        var passwordHash = mergePassword(request.password(), existing.getPasswordHash());
        return urlRepository.updateDetails(shortCode, orgId, longUrl, longUrlHash, expiresAt, folderId, startsAt, passwordHash);
    }

    // null = leave unchanged, empty string = clear the password, anything else = set a new password.
    private String mergePassword(String requestedPassword, String existingHash) {
        if (requestedPassword == null) {
            return existingHash;
        }
        return requestedPassword.isEmpty() ? null : passwordEncoder.encode(requestedPassword);
    }

    private Mono<Void> assignTagsIfPresent(Long orgId, String shortCode, List<String> tags) {
        if (tags == null) {
            return Mono.empty();
        }
        return urlRepository.findByShortCodeAndOrgId(shortCode, orgId)
                .flatMap(entity -> tagService.resolveOrCreate(orgId, tags)
                        .map(Tag::getId)
                        .collectList()
                        .flatMap(tagIds -> tagAssignmentDao.replaceTags(entity.getId(), tagIds)));
    }

    private Flux<UrlSummaryResponse> toResponses(List<UrlEntity> entities) {
        return Flux.fromIterable(entities).concatMap(this::toSummary);
    }

    private Mono<UrlSummaryResponse> toSummary(UrlEntity entity) {
        Mono<String> folderName = entity.getFolderId() == null
                ? Mono.just("")
                : folderRepository.findById(entity.getFolderId()).map(Folder::getName).defaultIfEmpty("");
        Mono<List<String>> tags = tagAssignmentDao.findTagNamesForUrl(entity.getId()).collectList();

        return Mono.zip(folderName, tags)
                .map(t -> new UrlSummaryResponse(
                        entity.getShortCode(),
                        baseUrl + "/" + entity.getShortCode(),
                        entity.getLongUrl(),
                        entity.getCreatedAt(),
                        entity.getExpiresAt(),
                        entity.getDisabledAt() != null,
                        t.getT1().isEmpty() ? null : t.getT1(),
                        t.getT2(),
                        entity.getStartsAt(),
                        entity.getPasswordHash() != null));
    }
}
