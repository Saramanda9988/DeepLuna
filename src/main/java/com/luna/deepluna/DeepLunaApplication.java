package com.luna.deepluna;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableJpaAuditing
@EnableTransactionManagement
@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.luna.deepluna.repository")
public class DeepLunaApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeepLunaApplication.class, args);
    }
}
