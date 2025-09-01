package com.back.fairytale.global.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource

@Configuration
open class GcsConfig(
    @Value("\${spring.cloud.gcp.storage.credentials.location}")
    private val keyFileLocation: String
) {

    @Bean
    fun storage(): Storage {
        val resource = ClassPathResource(keyFileLocation)
        val credentials = GoogleCredentials.fromStream(resource.inputStream)
        return StorageOptions.newBuilder()
            .setCredentials(credentials)
            .build()
            .service
    }
}