package com.luna.deepluna.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "app_config")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AppConfig {

    @Id
    @Column(name = "config_key", nullable = false, length = 128)
    private String configKey;

    @Column(name = "config_value", nullable = false, length = 512)
    private String configValue;

    @Column(name = "description", length = 255)
    private String description;

    @UpdateTimestamp
    @Column(name = "updated_time", nullable = false)
    private LocalDateTime updatedTime;
}

