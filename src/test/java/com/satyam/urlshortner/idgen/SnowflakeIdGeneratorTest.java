package com.satyam.urlshortner.idgen;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SnowflakeIdGeneratorTest {
    @Test
    void generatesUniqueIdsUnderConcurrency() throws InterruptedException {
        SnowflakeIdGenerator gen = new SnowflakeIdGenerator(1);
        Set<Long> ids = ConcurrentHashMap.newKeySet();
        int threads = 8, perThread = 10_000;

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        for (int t = 0; t < threads; t++) {
            pool.submit(() -> {
                for (int i = 0; i < perThread; i++) ids.add(gen.nextId());
            });
        }
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);

        assertEquals(threads * perThread, ids.size()); // zero collisions across 80,000 calls
    }
}
