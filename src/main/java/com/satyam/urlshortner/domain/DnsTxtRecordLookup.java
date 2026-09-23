package com.satyam.urlshortner.domain;

import reactor.core.publisher.Flux;

public interface DnsTxtRecordLookup {
    Flux<String> lookupTxtRecords(String domain);
}
