package com.luna.deepluna;

import com.luna.deepluna.common.exception.BusinessException;
import com.luna.deepluna.domain.entity.Model;
import com.luna.deepluna.repository.ModelRepository;
import com.luna.deepluna.service.ModelService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModelServiceTokenTest {

    @Mock
    private ModelRepository modelRepository;

    @InjectMocks
    private ModelService modelService;

    @Test
    void getModelEntityById_shouldReturnTokenForInternalUsage() {
        Model model = Model.builder()
                .modelId("m-1")
                .name("deepseek-chat")
                .url("https://api.deepseek.com")
                .token("token-123")
                .createTime(Instant.now())
                .build();
        when(modelRepository.findById("m-1")).thenReturn(Optional.of(model));

        Model actual = modelService.getModelEntityById("m-1");

        assertEquals("m-1", actual.getModelId());
        assertEquals("token-123", actual.getToken());
    }

    @Test
    void getModelEntityById_shouldThrowWhenMissing() {
        when(modelRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> modelService.getModelEntityById("missing"));
    }
}

