package com.luna.deepluna.cache;

import com.luna.deepluna.agent.context.SubAgentContext;
import com.luna.deepluna.agent.context.SupervisorAgentContext;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Component
public class ContextCache {
    // sessionId -> SupervisorAgentContext
    private final Map<String, SupervisorAgentContext> ongoingSupervisor = new ConcurrentHashMap<>();

    // sessionId -> List<SubAgentContext>
    private final Map<String, SubAgentContext> ongoingSubAgent = new ConcurrentHashMap<>();

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
    }
}
