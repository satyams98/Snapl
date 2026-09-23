package com.satyam.urlshortner.url;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UrlBulkActionService {

    private final UrlRepository urlRepository;
    private final FolderRepository folderRepository;
    private final TagService tagService;
    private final TagAssignmentDao tagAssignmentDao;

    public Mono<BulkActionResult> execute(Long orgId, BulkActionRequest request) {
        return switch (request.action()) {
            case DISABLE -> disableAll(orgId, request.shortCodes());
            case MOVE_TO_FOLDER -> moveToFolder(orgId, request);
            case ADD_TAGS -> addTags(orgId, request);
        };
    }

    private Mono<BulkActionResult> disableAll(Long orgId, List<String> codes) {
        return Flux.fromIterable(codes)
                .concatMap(code -> urlRepository.disableByShortCodeAndOrgId(code, orgId, Instant.now()))
                .reduce(0, (count, rows) -> count + (rows > 0 ? 1 : 0))
                .map(succeeded -> new BulkActionResult(codes.size(), succeeded));
    }

    private Mono<BulkActionResult> moveToFolder(Long orgId, BulkActionRequest request) {
        if (request.folderId() == null) {
            return Mono.error(new InvalidBulkActionException("folderId is required for MOVE_TO_FOLDER"));
        }
        return folderRepository.findByOrgIdAndId(orgId, request.folderId())
                .switchIfEmpty(Mono.error(new FolderNotFoundException(request.folderId())))
                .flatMap(folder -> Flux.fromIterable(request.shortCodes())
                        .concatMap(code -> urlRepository.updateFolderByShortCodeAndOrgId(code, orgId, folder.getId()))
                        .reduce(0, (count, rows) -> count + (rows > 0 ? 1 : 0))
                        .map(succeeded -> new BulkActionResult(request.shortCodes().size(), succeeded)));
    }

    private Mono<BulkActionResult> addTags(Long orgId, BulkActionRequest request) {
        if (request.tags() == null || request.tags().isEmpty()) {
            return Mono.error(new InvalidBulkActionException("tags are required for ADD_TAGS"));
        }
        return tagService.resolveOrCreate(orgId, request.tags())
                .map(Tag::getId)
                .collectList()
                .flatMap(tagIds -> Flux.fromIterable(request.shortCodes())
                        .concatMap(code -> urlRepository.findByShortCodeAndOrgId(code, orgId)
                                .flatMap(entity -> tagAssignmentDao.addTags(entity.getId(), tagIds).thenReturn(true))
                                .defaultIfEmpty(false))
                        .reduce(0, (count, ok) -> count + (ok ? 1 : 0))
                        .map(succeeded -> new BulkActionResult(request.shortCodes().size(), succeeded)));
    }
}
