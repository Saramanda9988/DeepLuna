package com.luna.deepluna.event.listener;

import com.luna.deepluna.agent.ReportGenerator;
import com.luna.deepluna.agent.SupervisorAgent;
import com.luna.deepluna.agent.context.SupervisorAgentContext;
import com.luna.deepluna.cache.ContextCache;
import com.luna.deepluna.cache.SessionCache;
import com.luna.deepluna.common.enums.SessionStatus;
import com.luna.deepluna.common.enums.SupervisorAgentState;
import com.luna.deepluna.common.utils.AssertUtil;
import com.luna.deepluna.domain.entity.Session;
import com.luna.deepluna.event.StartResearchEvent;
import com.luna.deepluna.repository.SessionRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@AllArgsConstructor
public class ResearchEventListener {

    private final SessionRepository sessionRepository;
    private final SupervisorAgent supervisorAgent;
    private final ContextCache contextCache;
    private final SessionCache sessionCache;
    private final ReportGenerator reportGenerator;

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMPLETION,
            classes = StartResearchEvent.class,
            fallbackExecution = true)
    public void startResearch(StartResearchEvent event) {
        String sessionId = event.getSessionId();
        Session session = sessionCache.getActiveSession(sessionId);
        AssertUtil.isNotNull(session, "找不到对应的Session，sessionId: " + sessionId);

        log.info("Starting research for sessionId: {}", session.getSessionId());
        // 2. 启动sub-agent执行具体的研究任务
        updateSessionStatus(session, SessionStatus.RUNNING);
        supervisorAgent.startResearch(session.getSessionId(), session.getResearchBrief());

        SupervisorAgentContext supervisor = contextCache.getSupervisor(session.getSessionId());
        AssertUtil.equal(supervisor.getStatus(), SupervisorAgentState.COMPLETED, "Supervisor Agent未完成研究任务");

        // 3. 生成报告总结
        updateSessionStatus(session, SessionStatus.REPORTING);
        reportGenerator.generateFinalReport(sessionId, supervisor);

        updateSessionStatus(session, SessionStatus.COMPLETED);
        log.info("Research completed for sessionId: {}", session.getSessionId());
    }

    private void updateSessionStatus(Session session, SessionStatus status) {
        log.info("Updating session {} status to {}", session.getSessionId(), status);
        AssertUtil.isNotNull(session, "Session不能为空");
        session.setStatus(status);

        // TODO: 使用@Async注解控制的受控线程池异步保存
        CompletableFuture.runAsync(() -> {
            sessionRepository.save(session);
            log.info("Session {} status updated to {}", session.getSessionId(), status);
        });
    }
}
