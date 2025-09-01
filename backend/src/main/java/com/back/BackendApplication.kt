package com.back

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@SpringBootApplication
//@EnableJpaAuditing 설정 파일 jpaConfig.kt로 이동
class BackendApplication

fun main(args: Array<String>) {
    runApplication<BackendApplication>(*args)
}
