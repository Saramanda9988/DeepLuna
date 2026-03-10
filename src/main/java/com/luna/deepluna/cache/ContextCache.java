package com.luna.deepluna.cache;

import com.luna.deepluna.agent.context.SubAgentContext;
import com.luna.deepluna.agent.context.SupervisorAgentContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;


@Component
public class ContextCache {
    // sessionId -> SupervisorAgentContext
    private final Map<String, SupervisorAgentContext> ongoingSupervisor = new ConcurrentHashMap<>();

    // subAgentId -> SubAgentContext
    private final Map<String, SubAgentContext> ongoingSubAgent = new ConcurrentHashMap<>();

    // sessionId -> subAgentIds
    private final ConcurrentMap<String, Set<String>> sessionSubAgents = new ConcurrentHashMap<>();

    public SupervisorAgentContext getSupervisor(String sessionId) {
        return ongoingSupervisor.get(sessionId);
    }

    public void putSupervisor(String sessionId, SupervisorAgentContext context) {
        ongoingSupervisor.put(sessionId, context);
    }

    public SubAgentContext getSubAgent(String subAgentId) {
        return ongoingSubAgent.get(subAgentId);
    }

    public void putSubAgent(String subAgentId, SubAgentContext context) {
        ongoingSubAgent.put(subAgentId, context);
        sessionSubAgents.computeIfAbsent(context.getSessionId(), key -> ConcurrentHashMap.newKeySet()).add(subAgentId);
    }

    public List<SubAgentContext> getSubAgentsBySessionId(String sessionId) {
        Set<String> subAgentIds = sessionSubAgents.get(sessionId);
        if (subAgentIds == null || subAgentIds.isEmpty()) {
            return List.of();
        }
        return subAgentIds.stream()
                .map(ongoingSubAgent::get)
                .filter(context -> context != null)
                .collect(Collectors.toList());
    }
}
