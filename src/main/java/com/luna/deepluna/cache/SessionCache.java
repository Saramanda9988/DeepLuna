package com.luna.deepluna.cache;

import com.luna.deepluna.domain.entity.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Component
public class SessionCache {
    // sessionId -> chat rounds
    private final ConcurrentMap<String, Integer> sessionChatRounds = new ConcurrentHashMap<>();

    // sessionId -> Session
    private final ConcurrentMap<String, Session> activeSessions = new ConcurrentHashMap<>();

    public Integer getChatRounds(String sessionId) {
        return sessionChatRounds.get(sessionId);
    }

    public void putChatRounds(String sessionId, Integer rounds) {
        sessionChatRounds.put(sessionId, rounds);
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
