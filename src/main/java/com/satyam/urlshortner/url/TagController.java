package com.satyam.urlshortner.url;

import com.satyam.urlshortner.auth.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @GetMapping
    public Flux<TagResponse> list() {
        return CurrentUser.get().flatMapMany(principal -> tagService.listForOrg(principal.orgId())).map(TagResponse::from);
    }
}
