package com.satyam.urlshortner.idgen;

public class SnowflakeIdGenerator {

    private static final long CUSTOM_EPOCH = 1704067200000L; // 2024-01-01T00:00:00Z

    private static final long WORKER_ID_BITS = 10L;
    private static final long SEQUENCE_BITS = 12L;

    private static final long MAX_WORKER_ID = (1L << WORKER_ID_BITS) - 1; // 1023
    private static final long MAX_SEQUENCE = (1L << SEQUENCE_BITS) - 1;   // 4095

    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;                  // 12
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS; // 22

    private final long workerId;
    private long lastTimestamp = -1L;
    private long sequence = 0L;

    public SnowflakeIdGenerator(long workerId) {
        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw new IllegalArgumentException("workerId must be between 0 and " + MAX_WORKER_ID);
        }
        this.workerId = workerId;
    }

    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();

        if (timestamp < lastTimestamp) {
            throw new IllegalStateException("Clock moved backwards. Refusing to generate id.");
        }

        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                while (timestamp <= lastTimestamp) {
                    timestamp = System.currentTimeMillis(); // spin until next ms
                }
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        return ((timestamp - CUSTOM_EPOCH) << TIMESTAMP_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }
}
