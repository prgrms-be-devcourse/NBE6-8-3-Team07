package com.back.fairytale.global.springDoc;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(title = "Fairytale API", version = "v1", description = "AI 기반 동화 생성 API 문서입니다."))
public class SpringDocConfig {
}
