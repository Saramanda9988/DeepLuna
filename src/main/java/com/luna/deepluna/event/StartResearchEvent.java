package com.luna.deepluna.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class StartResearchEvent extends ApplicationEvent {
    private final String sessionId;

    public StartResearchEvent(Object source, String sessionId) {
        super(source);
        this.sessionId = sessionId;
    }
}
