package com.luna.deepluna.cache;

import com.luna.deepluna.domain.entity.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntSupplier;

@Slf4j
@Component
public class SessionCache {
    // sessionId -> chat rounds
    private final ConcurrentMap<String, AtomicInteger> sessionChatRounds = new ConcurrentHashMap<>();

    // sessionId -> Session
    private final ConcurrentMap<String, Session> activeSessions = new ConcurrentHashMap<>();

    public Integer getChatRounds(String sessionId) {
        AtomicInteger rounds = sessionChatRounds.get(sessionId);
        return rounds == null ? null : rounds.get();
    }

    public void putChatRounds(String sessionId, Integer rounds) {
        if (sessionId == null || rounds == null) {
            log.warn("Attempted to put null sessionId or rounds into sessionChatRounds cache.");
            return;
        }
        sessionChatRounds.put(sessionId, new AtomicInteger(rounds));
    }

    public int nextChatRound(String sessionId, IntSupplier initialRoundSupplier) {
        if (sessionId == null || initialRoundSupplier == null) {
            throw new IllegalArgumentException("sessionId and initialRoundSupplier must not be null");
        }
        AtomicInteger counter = sessionChatRounds.computeIfAbsent(
                sessionId,
                key -> new AtomicInteger(initialRoundSupplier.getAsInt())
        );
        return counter.incrementAndGet();
    }

    public Session getActiveSession(String sessionId) {
        return activeSessions.get(sessionId);
    }

    public void putActiveSession(String sessionId, Session session) {
        if (sessionId == null || session == null) {
            log.warn("Attempted to put null sessionId or session into activeSessions cache.");
            return;
        }
        activeSessions.put(sessionId, session);
    }
}
