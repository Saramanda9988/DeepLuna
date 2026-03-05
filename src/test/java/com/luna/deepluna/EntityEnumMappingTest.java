package com.luna.deepluna;

import com.luna.deepluna.domain.entity.Session;
import com.luna.deepluna.domain.entity.Task;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityEnumMappingTest {

    @Test
    void sessionAndTaskEnums_shouldPersistAsString() throws NoSuchFieldException {
        Field sessionStatus = Session.class.getDeclaredField("status");
        Field taskAgentType = Task.class.getDeclaredField("agentType");
        Field taskStatus = Task.class.getDeclaredField("status");

        assertEquals(EnumType.STRING, sessionStatus.getAnnotation(Enumerated.class).value());
        assertEquals(EnumType.STRING, taskAgentType.getAnnotation(Enumerated.class).value());
        assertEquals(EnumType.STRING, taskStatus.getAnnotation(Enumerated.class).value());
    }

    @Test
    void taskFinishedTime_shouldAllowNull() throws NoSuchFieldException {
        Field finishedTime = Task.class.getDeclaredField("finishedTime");
        Column column = finishedTime.getAnnotation(Column.class);
        assertTrue(column.nullable());
    }
}

