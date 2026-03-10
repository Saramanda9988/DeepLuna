package com.luna.deepluna.service;

import com.luna.deepluna.agent.context.SubAgentContext;
import com.luna.deepluna.agent.context.SupervisorAgentContext;
import com.luna.deepluna.cache.ContextCache;
import com.luna.deepluna.cache.SessionCache;
import com.luna.deepluna.common.client.SseTransportClient;
import com.luna.deepluna.common.enums.SessionProgressEventType;
import com.luna.deepluna.common.enums.SessionStatus;
import com.luna.deepluna.common.enums.SubAgentTaskStatus;
import com.luna.deepluna.common.enums.SupervisorAgentState;
import com.luna.deepluna.common.exception.BusinessException;
import com.luna.deepluna.domain.entity.Session;
import com.luna.deepluna.domain.response.SessionProgressEventResponse;
import com.luna.deepluna.domain.response.SessionProgressSnapshotResponse;
import com.luna.deepluna.domain.response.SessionProgressSubAgentResponse;
import com.luna.deepluna.repository.SessionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
public class SessionProgressService {

    private static final long PROGRESS_STREAM_TIMEOUT_MS = 30L * 60L * 1000L;

    private final SessionRepository sessionRepository;
    private final SessionCache sessionCache;
    private final ContextCache contextCache;
    private final SseTransportClient sseTransportClient;

    private final ConcurrentMap<String, SessionProgressState> progressStates = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, CopyOnWriteArrayList<SseEmitter>> sessionEmitters = new ConcurrentHashMap<>();

    public SessionProgressService(
            SessionRepository sessionRepository,
            SessionCache sessionCache,
            ContextCache contextCache,
            SseTransportClient sseTransportClient
    ) {
        this.sessionRepository = sessionRepository;
        this.sessionCache = sessionCache;
        this.contextCache = contextCache;
        this.sseTransportClient = sseTransportClient;
    }

    public SessionProgressSnapshotResponse getSnapshot(String sessionId) {
        return getOrCreateState(sessionId).snapshot();
    }

    public SseEmitter subscribe(String sessionId) {
        SessionProgressState state = getOrCreateState(sessionId);
        SseEmitter emitter = sseTransportClient.createConnection(PROGRESS_STREAM_TIMEOUT_MS);

        sessionEmitters.computeIfAbsent(sessionId, key -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> removeEmitter(sessionId, emitter));
        emitter.onTimeout(() -> removeEmitter(sessionId, emitter));
        emitter.onError(error -> removeEmitter(sessionId, emitter));

        SessionProgressSnapshotResponse snapshot = state.snapshot();
        SessionProgressEventResponse initialEvent = SessionProgressEventResponse.builder()
                .sessionId(sessionId)
                .eventType(SessionProgressEventType.SNAPSHOT)
                .message("已建立研究进度连接")
                .sessionStatus(snapshot.getSessionStatus())
                .supervisorState(snapshot.getSupervisorState())
                .finished(snapshot.getFinished())
                .timestamp(LocalDateTime.now())
                .snapshot(snapshot)
                .build();
        try {
            sseTransportClient.sendEvent(emitter, "progress", initialEvent);
        } catch (Exception ex) {
            log.error("Failed to initialize progress stream for sessionId={}", sessionId, ex);
            sseTransportClient.handleError(emitter, ex);
        }

        if (Boolean.TRUE.equals(snapshot.getFinished())) {
            sseTransportClient.completeConnection(emitter);
        }

        return emitter;
    }

    public void publishSessionStatus(String sessionId, SessionStatus sessionStatus, String message) {
        SessionProgressEventResponse event = getOrCreateState(sessionId)
                .applySessionStatus(sessionStatus, message);
        broadcast(event);
    }

    public void publishSupervisorStatus(String sessionId, SupervisorAgentState supervisorState, String message) {
        SessionProgressEventResponse event = getOrCreateState(sessionId)
                .applySupervisorStatus(supervisorState, message);
        broadcast(event);
    }

    public void publishSubAgentCreated(String sessionId, String subAgentId, String researchTopic) {
        SessionProgressEventResponse event = getOrCreateState(sessionId)
                .applySubAgentCreated(subAgentId, researchTopic);
        broadcast(event);
    }

    public void publishSubAgentStatus(
            String sessionId,
            String subAgentId,
            String researchTopic,
            SubAgentTaskStatus status,
            String message
    ) {
        SessionProgressEventResponse event = getOrCreateState(sessionId)
                .applySubAgentStatus(subAgentId, researchTopic, status, message);
        broadcast(event);
    }

    public void publishMessage(String sessionId, SessionProgressEventType eventType, String message) {
        SessionProgressEventResponse event = getOrCreateState(sessionId)
                .applyMessage(eventType, message);
        broadcast(event);
    }

    private void broadcast(SessionProgressEventResponse event) {
        CopyOnWriteArrayList<SseEmitter> emitters = sessionEmitters.get(event.getSessionId());
        if (emitters != null) {
            for (SseEmitter emitter : emitters) {
                try {
                    sseTransportClient.sendEvent(emitter, "progress", event);
                } catch (Exception ex) {
                    log.warn("Failed to push progress event for sessionId={}", event.getSessionId(), ex);
                    removeEmitter(event.getSessionId(), emitter);
                }
            }
        }

        if (Boolean.TRUE.equals(event.getFinished())) {
            completeEmitters(event.getSessionId());
        }
    }

    private void completeEmitters(String sessionId) {
        CopyOnWriteArrayList<SseEmitter> emitters = sessionEmitters.remove(sessionId);
        if (emitters == null) {
            return;
        }
        for (SseEmitter emitter : emitters) {
            sseTransportClient.completeConnection(emitter);
        }
    }

    private void removeEmitter(String sessionId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> emitters = sessionEmitters.get(sessionId);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            sessionEmitters.remove(sessionId, emitters);
        }
    }

    private SessionProgressState getOrCreateState(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new BusinessException("SessionId不能为空");
        }
        return progressStates.computeIfAbsent(sessionId, this::loadState);
    }

    private SessionProgressState loadState(String sessionId) {
        Session session = Optional.ofNullable(sessionCache.getActiveSession(sessionId))
                .orElseGet(() -> sessionRepository.findById(sessionId)
                        .map(entity -> {
                            sessionCache.putActiveSession(sessionId, entity);
                            return entity;
                        })
                        .orElseThrow(() -> new BusinessException("Session不存在")));

        SupervisorAgentContext supervisorContext = contextCache.getSupervisor(sessionId);
        List<SubAgentContext> subAgentContexts = contextCache.getSubAgentsBySessionId(sessionId);

        SessionProgressState state = new SessionProgressState(sessionId);
        state.sessionStatus = session.getStatus();
        state.supervisorState = supervisorContext != null ? supervisorContext.getStatus() : null;
        state.updatedAt = session.getUpdateTime() != null ? session.getUpdateTime() : LocalDateTime.now();
        state.latestMessage = "会话已初始化";

        for (SubAgentContext subAgentContext : subAgentContexts) {
            SessionProgressSubAgentResponse subAgent = SessionProgressSubAgentResponse.builder()
                    .subAgentId(subAgentContext.getSubAgentId())
                    .researchTopic(subAgentContext.getResearchTopic())
                    .status(subAgentContext.getStatus())
                    .updatedAt(LocalDateTime.now())
                    .build();
            state.subAgents.put(subAgent.getSubAgentId(), subAgent);
        }
        return state;
    }

    private static final class SessionProgressState {

        private final String sessionId;
        private SessionStatus sessionStatus;
        private SupervisorAgentState supervisorState;
        private String latestMessage;
        private LocalDateTime updatedAt;
        private final Map<String, SessionProgressSubAgentResponse> subAgents = new LinkedHashMap<>();

        private SessionProgressState(String sessionId) {
            this.sessionId = sessionId;
            this.updatedAt = LocalDateTime.now();
        }

        private synchronized SessionProgressEventResponse applySessionStatus(SessionStatus status, String message) {
            sessionStatus = status;
            touch(message);
            return buildEvent(SessionProgressEventType.SESSION_STATUS_CHANGED, message, null);
        }

        private synchronized SessionProgressEventResponse applySupervisorStatus(
                SupervisorAgentState status,
                String message
        ) {
            supervisorState = status;
            touch(message);
            return buildEvent(SessionProgressEventType.SUPERVISOR_STATUS_CHANGED, message, null);
        }

        private synchronized SessionProgressEventResponse applySubAgentCreated(
                String subAgentId,
                String researchTopic
        ) {
            LocalDateTime now = LocalDateTime.now();
            SessionProgressSubAgentResponse subAgent = SessionProgressSubAgentResponse.builder()
                    .subAgentId(subAgentId)
                    .researchTopic(researchTopic)
                    .status(SubAgentTaskStatus.PENDING)
                    .updatedAt(now)
                    .build();
            subAgents.put(subAgentId, subAgent);
            touch("已创建子任务: " + researchTopic);
            return buildEvent(SessionProgressEventType.SUB_AGENT_CREATED, latestMessage, subAgent);
        }

        private synchronized SessionProgressEventResponse applySubAgentStatus(
                String subAgentId,
                String researchTopic,
                SubAgentTaskStatus status,
                String message
        ) {
            LocalDateTime now = LocalDateTime.now();
            SessionProgressSubAgentResponse subAgent = SessionProgressSubAgentResponse.builder()
                    .subAgentId(subAgentId)
                    .researchTopic(researchTopic)
                    .status(status)
                    .updatedAt(now)
                    .build();
            subAgents.put(subAgentId, subAgent);
            touch(message);
            return buildEvent(SessionProgressEventType.SUB_AGENT_STATUS_CHANGED, message, subAgent);
        }

        private synchronized SessionProgressEventResponse applyMessage(
                SessionProgressEventType eventType,
                String message
        ) {
            touch(message);
            return buildEvent(eventType, message, null);
        }

        private synchronized SessionProgressSnapshotResponse snapshot() {
            List<SessionProgressSubAgentResponse> subAgentList = new ArrayList<>(subAgents.values());
            subAgentList.sort(Comparator.comparing(
                    SessionProgressSubAgentResponse::getUpdatedAt,
                    Comparator.nullsLast(Comparator.reverseOrder())
            ));

            int total = subAgentList.size();
            int running = (int) subAgentList.stream()
                    .filter(subAgent -> subAgent.getStatus() == SubAgentTaskStatus.IN_PROGRESS)
                    .count();
            int completed = (int) subAgentList.stream()
                    .filter(subAgent -> subAgent.getStatus() == SubAgentTaskStatus.COMPLETED)
                    .count();

            return SessionProgressSnapshotResponse.builder()
                    .sessionId(sessionId)
                    .sessionStatus(sessionStatus)
                    .supervisorState(supervisorState)
                    .latestMessage(latestMessage)
                    .updatedAt(updatedAt)
                    .totalSubAgents(total)
                    .runningSubAgents(running)
                    .completedSubAgents(completed)
                    .finished(isFinished())
                    .subAgents(subAgentList)
                    .build();
        }

        private SessionProgressEventResponse buildEvent(
                SessionProgressEventType eventType,
                String message,
                SessionProgressSubAgentResponse subAgent
        ) {
            SessionProgressSnapshotResponse snapshot = snapshot();
            return SessionProgressEventResponse.builder()
                    .sessionId(sessionId)
                    .eventType(eventType)
                    .message(message)
                    .sessionStatus(sessionStatus)
                    .supervisorState(supervisorState)
                    .subAgent(subAgent)
                    .finished(snapshot.getFinished())
                    .timestamp(updatedAt)
                    .snapshot(snapshot)
                    .build();
        }

        private void touch(String message) {
            if (message != null && !message.isBlank()) {
                latestMessage = message;
            }
            updatedAt = LocalDateTime.now();
        }

        private boolean isFinished() {
            return sessionStatus == SessionStatus.COMPLETED || sessionStatus == SessionStatus.FAILED;
        }
    }
}
