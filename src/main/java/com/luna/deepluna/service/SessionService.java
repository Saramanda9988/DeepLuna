package com.luna.deepluna.service;

import com.luna.deepluna.common.enums.SessionStatus;
import com.luna.deepluna.dto.request.CreateSessionRequest;
import com.luna.deepluna.dto.request.UpdateSessionRequest;
import com.luna.deepluna.dto.response.SessionDetailResponse;
import com.luna.deepluna.dto.response.SessionResponse;
import com.luna.deepluna.dto.entity.Session;
import com.luna.deepluna.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {
    
    private final SessionRepository sessionRepository;
    
    /**
     * 创建Session
     */
    public String createSession(CreateSessionRequest request) {
        String sessionId = UUID.randomUUID().toString();
        
        Session session = Session.builder()
                .sessionId(sessionId)
                .userId(request.getUserId())
                .model(request.getModel())
                .status(SessionStatus.INIT)
                .build();
        
        sessionRepository.save(session);
        
        log.info("创建Session成功: sessionId={}, userId={}, model={}", 
                sessionId, request.getUserId(), request.getModel());
        
        return sessionId;
    }
    
    /**
     * 查询用户历史Session列表
     */
    public List<SessionResponse> getUserSessions(Long userId) {
        List<Session> sessions = sessionRepository.findByUserId(userId);
        
        return sessions.stream()
                .map(session -> new SessionResponse(session.getSessionId(), session.getSummary()))
                .collect(Collectors.toList());
    }
    
    /**
     * 更新Session内容
     */
    public void updateSession(String sessionId, UpdateSessionRequest request) {
        Optional<Session> optionalSession = sessionRepository.findById(sessionId);
        if (optionalSession.isEmpty()) {
            throw new RuntimeException("Session不存在");
        }
        
        Session session = optionalSession.get();
        
        if (request.getSummary() != null) {
            session.setSummary(request.getSummary());
        }
        
        if (request.getResearchBrief() != null) {
            session.setResearchBrief(request.getResearchBrief());
        }
        
        sessionRepository.save(session);
        
        log.info("更新Session成功: sessionId={}", sessionId);
    }
    
    /**
     * 删除Session
     */
    public void deleteSession(String sessionId) {
        Optional<Session> optionalSession = sessionRepository.findById(sessionId);
        if (optionalSession.isEmpty()) {
            throw new RuntimeException("Session不存在");
        }
        
        sessionRepository.deleteById(sessionId);
        
        log.info("删除Session成功: sessionId={}", sessionId);
    }
    
    /**
     * 获取Session详情
     */
    public SessionDetailResponse getSessionDetail(String sessionId) {
        Optional<Session> optionalSession = sessionRepository.findById(sessionId);
        if (optionalSession.isEmpty()) {
            throw new RuntimeException("Session不存在");
        }
        
        Session session = optionalSession.get();
        
        return new SessionDetailResponse(
                session.getSessionId(),
                session.getUserId(),
                session.getModel(),
                session.getSummary(),
                session.getStatus(),
                session.getResearchBrief(),
                session.getCreatedTime(),
                session.getUpdateTime()
        );
    }
}
