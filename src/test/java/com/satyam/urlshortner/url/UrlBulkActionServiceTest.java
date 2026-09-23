package com.satyam.urlshortner.url;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlBulkActionServiceTest {

    @Mock
    private UrlRepository urlRepository;
    @Mock
    private FolderRepository folderRepository;
    @Mock
    private TagService tagService;
    @Mock
    private TagAssignmentDao tagAssignmentDao;

    private UrlBulkActionService service;

    private static final Long ORG_ID = 1L;

    @BeforeEach
    void setUp() {
        service = new UrlBulkActionService(urlRepository, folderRepository, tagService, tagAssignmentDao);
    }

    @Test
    void disableCountsOnlyRowsThatWereActuallyUpdated() {
        when(urlRepository.disableByShortCodeAndOrgId(eq("a"), eq(ORG_ID), any(Instant.class))).thenReturn(Mono.just(1));
        when(urlRepository.disableByShortCodeAndOrgId(eq("missing"), eq(ORG_ID), any(Instant.class))).thenReturn(Mono.just(0));

        BulkActionRequest request = new BulkActionRequest(List.of("a", "missing"), BulkAction.DISABLE, null, null);

        StepVerifier.create(service.execute(ORG_ID, request))
                .assertNext(result -> {
                    assertEquals(2, result.requested());
                    assertEquals(1, result.succeeded());
                })
                .verifyComplete();
    }

    @Test
    void moveToFolderFailsWithoutFolderId() {
        BulkActionRequest request = new BulkActionRequest(List.of("a"), BulkAction.MOVE_TO_FOLDER, null, null);

        StepVerifier.create(service.execute(ORG_ID, request))
                .expectError(InvalidBulkActionException.class)
                .verify();

        verifyNoInteractions(folderRepository);
    }

    @Test
    void moveToFolderFailsWhenFolderNotFound() {
        when(folderRepository.findByOrgIdAndId(ORG_ID, 5L)).thenReturn(Mono.empty());

        BulkActionRequest request = new BulkActionRequest(List.of("a"), BulkAction.MOVE_TO_FOLDER, 5L, null);

        StepVerifier.create(service.execute(ORG_ID, request))
                .expectError(FolderNotFoundException.class)
                .verify();
    }

    @Test
    void moveToFolderUpdatesEachCode() {
        Folder folder = new Folder(5L, ORG_ID, "Campaigns", Instant.now());
        when(folderRepository.findByOrgIdAndId(ORG_ID, 5L)).thenReturn(Mono.just(folder));
        when(urlRepository.updateFolderByShortCodeAndOrgId(anyString(), eq(ORG_ID), eq(5L))).thenReturn(Mono.just(1));

        BulkActionRequest request = new BulkActionRequest(List.of("a", "b"), BulkAction.MOVE_TO_FOLDER, 5L, null);

        StepVerifier.create(service.execute(ORG_ID, request))
                .assertNext(result -> assertEquals(2, result.succeeded()))
                .verifyComplete();
    }

    @Test
    void addTagsFailsWithoutTags() {
        BulkActionRequest request = new BulkActionRequest(List.of("a"), BulkAction.ADD_TAGS, null, List.of());

        StepVerifier.create(service.execute(ORG_ID, request))
                .expectError(InvalidBulkActionException.class)
                .verify();
    }

    @Test
    void addTagsResolvesTagsAndAssignsToEachUrl() {
        Tag tag = new Tag(9L, ORG_ID, "campaign", Instant.now());
        UrlEntity entity = new UrlEntity(1L, "a", "https://example.com", "hash", Instant.now(), null, false, null, ORG_ID, null, null, null);

        when(tagService.resolveOrCreate(ORG_ID, List.of("campaign"))).thenReturn(Flux.just(tag));
        when(urlRepository.findByShortCodeAndOrgId("a", ORG_ID)).thenReturn(Mono.just(entity));
        when(tagAssignmentDao.addTags(1L, List.of(9L))).thenReturn(Mono.empty());

        BulkActionRequest request = new BulkActionRequest(List.of("a"), BulkAction.ADD_TAGS, null, List.of("campaign"));

        StepVerifier.create(service.execute(ORG_ID, request))
                .assertNext(result -> {
                    assertEquals(1, result.requested());
                    assertEquals(1, result.succeeded());
                })
                .verifyComplete();
    }
}

