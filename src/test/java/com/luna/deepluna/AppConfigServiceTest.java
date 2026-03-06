package com.luna.deepluna;

import com.luna.deepluna.domain.entity.AppConfig;
import com.luna.deepluna.repository.AppConfigRepository;
import com.luna.deepluna.service.AppConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppConfigServiceTest {

    @Mock
    private AppConfigRepository appConfigRepository;

    @InjectMocks
    private AppConfigService appConfigService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(appConfigService, "webSearchProviderDefault", "tavily");
    }

    @Test
    void getWebSearchProvider_shouldFallbackToPropertyWhenDbMissing() {
        when(appConfigRepository.findById(AppConfigService.WEBSEARCH_PROVIDER_KEY)).thenReturn(Optional.empty());

        String provider = appConfigService.getWebSearchProvider();

        assertEquals("tavily", provider);
    }

    @Test
    void setWebSearchProvider_shouldPersistNormalizedProviderId() {
        when(appConfigRepository.findById(AppConfigService.WEBSEARCH_PROVIDER_KEY)).thenReturn(Optional.empty());

        appConfigService.setWebSearchProvider(" Tavily ");

        ArgumentCaptor<AppConfig> captor = ArgumentCaptor.forClass(AppConfig.class);
        verify(appConfigRepository).save(captor.capture());

        assertEquals(AppConfigService.WEBSEARCH_PROVIDER_KEY, captor.getValue().getConfigKey());
        assertEquals("tavily", captor.getValue().getConfigValue());
    }
}

