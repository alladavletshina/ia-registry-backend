package com.asset.assets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class AssetSecurityConfig {
    
    private static final Logger log = LoggerFactory.getLogger(AssetSecurityConfig.class);
    
    public AssetSecurityConfig() {
        log.info("🚀🚀🚀🚀🚀 ASSET SECURITY CONFIG LOADED! 🚀🚀🚀🚀🚀");
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        log.info("⚙️⚙️⚙️ CONFIGURING SECURITY FILTER CHAIN ⚙️⚙️⚙️");
        
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(authz -> authz
                .requestMatchers(
                    "/api/assets/health",
                    "/api/assets/test", 
                    "/api/assets/db-check",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/api-docs/**",
                    "/v3/api-docs/**"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> {
                    log.info("🔐 Configuring JWT decoder");
                    jwt.decoder(jwtDecoder());
                })
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            );
        
        return http.build();
    }
    
    @Bean
    public JwtDecoder jwtDecoder() {
        log.info("🔑 Creating JWT Decoder for Keycloak");
        String jwkSetUri = "http://keycloak:8080/realms/asset-management/protocol/openid-connect/certs";
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }
}
