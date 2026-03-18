package com.luna.deepluna.agent.agentTool;

import com.luna.deepluna.agent.SubAgent;
import com.luna.deepluna.common.utils.AssertUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SupervisorTools {

    private final SubAgent subAgent;

    @Tool(description = "将研究任务委派给专业子智能体（异步执行），返回subAgentId")
    public String conductResearch(@ToolParam(description = "子智能体的研究主题") String researchTopic,
                                  ToolContext toolContext) {
        String sessionId = (String) toolContext.getContext().get("sessionId");
        AssertUtil.isNotEmpty(sessionId, "Supervisor tool context缺少sessionId");
        return subAgent.startSubAgentResearch(sessionId, researchTopic);
    }

    @Tool(description = "表明研究已完成")
    public void researchComplete() {

    }

    @Tool(description = "查询指定子智能体的状态和结果")
    public Map<String, Object> checkSubAgentStatus(
            @ToolParam(description = "子智能体ID") String subAgentId
    ) {
        return subAgent.getSubAgentStatus(subAgentId);
    }

    @Tool(description = "查询当前会话下全部子智能体状态")
    public List<Map<String, Object>> listSubAgents(ToolContext toolContext) {
        String sessionId = (String) toolContext.getContext().get("sessionId");
        AssertUtil.isNotEmpty(sessionId, "Supervisor tool context缺少sessionId");
        return subAgent.listSessionSubAgentStatuses(sessionId);
    }

    @Tool(description = "阻塞等待任意一个子任务完成或失败，返回该任务结果摘要")
    public Map<String, Object> waitForAnySubAgentResult(
            @ToolParam(description = "等待超时时间毫秒，例如 120000") Long timeoutMs,
            ToolContext toolContext
    ) {
        String sessionId = (String) toolContext.getContext().get("sessionId");
        AssertUtil.isNotEmpty(sessionId, "Supervisor tool context缺少sessionId");
        return subAgent.waitForAnySubAgentResult(sessionId, timeoutMs == null ? 120000L : timeoutMs);
    }

    @Tool(description = "重启一个失败的子任务，返回新的subAgentId")
    public String restartFailedSubAgent(
            @ToolParam(description = "失败的子智能体ID") String failedSubAgentId,
            ToolContext toolContext
    ) {
        String sessionId = (String) toolContext.getContext().get("sessionId");
        AssertUtil.isNotEmpty(sessionId, "Supervisor tool context缺少sessionId");
        return subAgent.restartFailedSubAgent(sessionId, failedSubAgentId);
    }

    @Tool(description = "获取指定已完成子任务的完整研究结果")
    public String getSubAgentResult(
            @ToolParam(description = "已完成的子智能体ID") String subAgentId
    ) {
        return subAgent.getSubAgentResult(subAgentId);
    }

    @Tool(description = "停止指定仍在运行的子任务")
    public void stopSubAgent(
            @ToolParam(description = "需要停止的子智能体ID") String subAgentId
    ) {
        subAgent.cancelSubAgent(subAgentId);
    }

    @Tool(description = "停止当前会话下所有仍在运行的子任务，并返回停止数量")
    public Integer stopAllRunningSubAgents(ToolContext toolContext) {
        String sessionId = (String) toolContext.getContext().get("sessionId");
        AssertUtil.isNotEmpty(sessionId, "Supervisor tool context缺少sessionId");
        return subAgent.cancelRunningSubAgents(sessionId);
    }

    @Tool(description = "用于研究过程中的反思与策略规划")
    public String thinkTool(@ToolParam(description = "监督者智能体的反思内容") String reflectionInput) {
        return "[Reflection Result] " + reflectionInput;
    }
}
