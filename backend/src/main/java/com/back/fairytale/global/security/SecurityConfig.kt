package com.back.fairytale.global.security

import com.back.fairytale.global.security.jwt.JwtAuthenticationFilter
import com.back.fairytale.global.security.oauth2.*
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource


@EnableConfigurationProperties(CorsProperties::class)
@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val customOAuth2UserService: CustomOAuth2UserService,
    private val oAuth2LoginSuccessHandler: OAuth2LoginSuccessHandler,
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val logoutHandler: CustomLogoutHandler,
    private val customLogoutSuccessHandler: CustomLogoutSuccessHandler,
    private val customAuthenticationEntryPoint: CustomAuthenticationEntryPoint,
    private val corsProperties: CorsProperties
) {
    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        val allowedPaths = arrayOf("/", "/login", "/oauth2/**", "/h2-console/**", "/favicon.ico", "/swagger-ui/**")
        return http
            .authorizeHttpRequests { auth ->
                auth
                    // 허용된 경로들
                    .requestMatchers(*allowedPaths).permitAll()
                    .requestMatchers("/fairytales/public").permitAll()
                    .requestMatchers(HttpMethod.POST, "/reissue").permitAll()
                    .requestMatchers(HttpMethod.POST, "/logout").permitAll()
                    // 성능 테스트를 위한 검색 API 임시 허용
                    .requestMatchers(HttpMethod.GET, "/api/fairytales/search").permitAll()

                    // 인증이 필요한 경로들
                    .requestMatchers("/api/**").hasAnyAuthority("ROLE_USER", "ROLE_ADMIN")
                    .requestMatchers("/fairytales/**").hasAnyAuthority("ROLE_USER", "ROLE_ADMIN")
                    .requestMatchers("/bookmark/**").hasAnyAuthority("ROLE_USER", "ROLE_ADMIN")
                    .requestMatchers("/bookmarks/**").hasAnyAuthority("ROLE_USER", "ROLE_ADMIN")
                    .requestMatchers("/likes/**").hasAnyAuthority("ROLE_USER", "ROLE_ADMIN")
                    .requestMatchers("/users/**").hasAnyAuthority("ROLE_USER", "ROLE_ADMIN")

                    // 나머지 모든 요청은 인증 필요
                    .anyRequest().authenticated()
            }
            .csrf { csrf -> csrf.disable() }
            .formLogin { form -> form.disable() }
            .httpBasic { basic -> basic.disable() }
            .sessionManagement { session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .headers { headers ->
                headers.frameOptions { frame -> frame.sameOrigin() }
            }
            .cors { cors ->
                cors.configurationSource(corsConfigurationSource())
            }
            .oauth2Login { oauth2 ->
                oauth2
                    .userInfoEndpoint { userInfo ->
                        userInfo.userService(customOAuth2UserService)
                    }
                    .successHandler(oAuth2LoginSuccessHandler)
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .exceptionHandling { exception ->
                exception.authenticationEntryPoint(customAuthenticationEntryPoint)
            }
            .logout { logout ->
                logout
                    .addLogoutHandler(logoutHandler)
                    .logoutSuccessHandler(customLogoutSuccessHandler)
            }
            .build()
    }

    @Bean
    fun webSecurityCustomizer(): WebSecurityCustomizer {
        return WebSecurityCustomizer { web ->
            web.ignoring().requestMatchers("/h2-console/**", "/favicon.ico")
        }
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration().apply {
            allowedOrigins = corsProperties.allowedOrigins
            allowedMethods = corsProperties.allowedMethods
            allowedHeaders = corsProperties.allowedHeaders
            allowCredentials = corsProperties.allowCredentials
            exposedHeaders = corsProperties.exposedHeaders
        }
        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", configuration)
        }
    }
}