package com.satyam.urlshortner.url;

import com.satyam.urlshortner.auth.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@RestController
@RequestMapping("/api/folders")
@RequiredArgsConstructor
public class FolderController {

    private final FolderRepository folderRepository;

    @GetMapping
    public Flux<FolderResponse> list() {
        return CurrentUser.get()
                .flatMapMany(principal -> folderRepository.findByOrgIdOrderByNameAsc(principal.orgId()))
                .map(FolderResponse::from);
    }

    @PostMapping
    public Mono<FolderResponse> create(@Valid @RequestBody CreateFolderRequest request) {
        return CurrentUser.get()
                .flatMap(principal -> folderRepository.findByOrgIdAndName(principal.orgId(), request.name())
                        .switchIfEmpty(Mono.defer(() ->
                                folderRepository.save(new Folder(null, principal.orgId(), request.name(), Instant.now())))))
                .map(FolderResponse::from);
    }
}
