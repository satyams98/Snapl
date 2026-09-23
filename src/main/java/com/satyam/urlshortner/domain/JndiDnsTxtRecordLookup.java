package com.satyam.urlshortner.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

// Uses the JDK's built-in JNDI DNS provider (no extra dependency) to read TXT records for domain verification.
@Component
public class JndiDnsTxtRecordLookup implements DnsTxtRecordLookup {

    private static final Logger log = LoggerFactory.getLogger(JndiDnsTxtRecordLookup.class);

    @Override
    public Flux<String> lookupTxtRecords(String domain) {
        return Mono.fromCallable(() -> queryTxtRecords(domain))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(Flux::fromIterable)
                .onErrorResume(ex -> {
                    log.warn("DNS TXT lookup failed for {}: {}", domain, ex.getMessage());
                    return Flux.empty();
                });
    }

    private List<String> queryTxtRecords(String domain) throws Exception {
        Hashtable<String, String> env = new Hashtable<>();
        env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
        DirContext context = new InitialDirContext(env);
        try {
            Attributes attributes = context.getAttributes(domain, new String[]{"TXT"});
            Attribute txt = attributes.get("TXT");
            List<String> values = new ArrayList<>();
            if (txt != null) {
                NamingEnumeration<?> all = txt.getAll();
                while (all.hasMore()) {
                    values.add(String.valueOf(all.next()).replace("\"", ""));
                }
            }
            return values;
        } finally {
            context.close();
        }
    }
}
