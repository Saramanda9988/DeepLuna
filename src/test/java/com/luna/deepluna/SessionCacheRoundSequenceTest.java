package com.luna.deepluna;

import com.luna.deepluna.cache.SessionCache;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionCacheRoundSequenceTest {

    @Test
    void nextChatRound_shouldBeStrictlyIncreasingUnderConcurrency() throws InterruptedException {
        SessionCache sessionCache = new SessionCache();
        String sessionId = "session-round-1";
        int taskCount = 100;

        ExecutorService pool = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(taskCount);
        Set<Integer> rounds = ConcurrentHashMap.newKeySet();
        AtomicInteger initCalls = new AtomicInteger(0);

        for (int i = 0; i < taskCount; i++) {
            pool.submit(() -> {
                try {
                    int round = sessionCache.nextChatRound(sessionId, () -> {
                        initCalls.incrementAndGet();
                        return 0;
                    });
                    rounds.add(round);
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean finished = latch.await(10, TimeUnit.SECONDS);
        pool.shutdownNow();

        assertTrue(finished, "concurrency test timeout");
        assertEquals(taskCount, rounds.size());
        assertEquals(1, initCalls.get());
        assertTrue(rounds.contains(1));
        assertTrue(rounds.contains(taskCount));
    }
}

