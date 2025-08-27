package com.back.fairytale.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

@Configuration
public class GcsConfig {

    @Value("${spring.cloud.gcp.storage.credentials.location}")
    private String keyFileLocation;

    @Bean
    public Storage storage() throws IOException {
        ClassPathResource resource = new ClassPathResource(keyFileLocation);
        GoogleCredentials credentials = GoogleCredentials.fromStream(resource.getInputStream());
        return StorageOptions.newBuilder().setCredentials(credentials).build().getService();
    }
}