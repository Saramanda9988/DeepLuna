package com.luna.deepluna;

import com.luna.deepluna.agent.SubAgent;
import com.luna.deepluna.agent.agentTool.SupervisorTools;
import com.luna.deepluna.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ToolContext;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupervisorToolsSessionRoutingTest {

    @Mock
    private SubAgent subAgent;

    @Mock
    private ToolContext toolContext;

    private SupervisorTools supervisorTools;

    @BeforeEach
    void setUp() {
        supervisorTools = new SupervisorTools(subAgent);
    }

    @Test
    void conductResearch_shouldRouteWithSessionIdFromToolContext() {
        Map<String, Object> context = new HashMap<>();
        context.put("sessionId", "session-001");
        when(toolContext.getContext()).thenReturn(context);
        when(subAgent.startSubAgentResearch("session-001", "AI trend")).thenReturn("ok");

        String result = supervisorTools.conductResearch("AI trend", toolContext);

        assertEquals("ok", result);
        verify(subAgent).startSubAgentResearch("session-001", "AI trend");
    }

    @Test
    void conductResearch_shouldFailWhenSessionIdMissing() {
        when(toolContext.getContext()).thenReturn(Map.of());

        assertThrows(BusinessException.class, () -> supervisorTools.conductResearch("AI trend", toolContext));
    }
}

