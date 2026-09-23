package com.satyam.urlshortner.url;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Query;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UrlQueryServiceTest {

    @Mock
    private R2dbcEntityTemplate entityTemplate;
    @Mock
    private UrlRepository urlRepository;
    @Mock
    private FolderRepository folderRepository;
    @Mock
    private TagRepository tagRepository;
    @Mock
    private TagAssignmentDao tagAssignmentDao;
    @Mock
    private TagService tagService;
    @Mock
    private UrlService urlService;
    @Mock
    private PasswordEncoder passwordEncoder;

    private UrlQueryService urlQueryService;

    private static final Long ORG_ID = 1L;

    @BeforeEach
    void setUp() {
        urlQueryService = new UrlQueryService(entityTemplate, urlRepository, folderRepository, tagRepository, tagAssignmentDao, tagService, urlService, passwordEncoder);
        ReflectionTestUtils.setField(urlQueryService, "baseUrl", "http://localhost:8080");
    }

    @Test
    void listReturnsSummariesWithFolderAndTags() {
        UrlEntity entity = new UrlEntity(1L, "abc123", "https://example.com", "hash", Instant.now(), null, false, null, ORG_ID, 7L, null, null);

        when(entityTemplate.select(any(Query.class), eq(UrlEntity.class))).thenReturn(Flux.just(entity));
        when(entityTemplate.count(any(Query.class), eq(UrlEntity.class))).thenReturn(Mono.just(1L));
        when(folderRepository.findById(7L)).thenReturn(Mono.just(new Folder(7L, ORG_ID, "Campaigns", Instant.now())));
        when(tagAssignmentDao.findTagNamesForUrl(1L)).thenReturn(Flux.just("launch"));

        StepVerifier.create(urlQueryService.list(ORG_ID, null, null, null, null, 0, 20))
                .assertNext(response -> {
                    assertEquals(1, response.totalCount());
                    assertEquals("abc123", response.items().get(0).shortCode());
                    assertEquals("Campaigns", response.items().get(0).folderName());
                    assertEquals(List.of("launch"), response.items().get(0).tags());
                })
                .verifyComplete();
    }

    @Test
    void getDetailFailsWhenNotFoundInOrg() {
        when(urlRepository.findByShortCodeAndOrgId("missing", ORG_ID)).thenReturn(Mono.empty());

        StepVerifier.create(urlQueryService.getDetail(ORG_ID, "missing"))
                .expectError(ShortUrlNotFoundException.class)
                .verify();
    }

    @Test
    void updateAppliesOnlyProvidedFieldsAndLeavesOthersUnchanged() {
        UrlEntity existing = new UrlEntity(1L, "abc123", "https://old.example.com", "oldhash", Instant.now(), null, false, null, ORG_ID, null, null, null);
        UpdateUrlRequest request = new UpdateUrlRequest(null, null, 3L, null, null, null);

        when(urlRepository.findByShortCodeAndOrgId("abc123", ORG_ID)).thenReturn(Mono.just(existing));
        when(urlRepository.updateDetails(eq("abc123"), eq(ORG_ID), eq("https://old.example.com"), eq("oldhash"), isNull(), eq(3L), isNull(), isNull()))
                .thenReturn(Mono.just(1));
        when(tagAssignmentDao.findTagNamesForUrl(1L)).thenReturn(Flux.empty());
        when(urlService.evictCache("abc123")).thenReturn(Mono.empty());

        StepVerifier.create(urlQueryService.update(ORG_ID, "abc123", request))
                .assertNext(response -> assertEquals(List.of(), response.tags()))
                .verifyComplete();
    }
}
