package com.asset.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.net.URI;
import java.util.Collections;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@Configuration
public class SwaggerUiConfig {

    @Bean
    public RouterFunction<ServerResponse> swaggerRoutes() {
        return route(GET("/swagger-ui.html"),
                req -> ServerResponse.temporaryRedirect(URI.create("/webjars/swagger-ui/index.html"))
                        .build())
                .andRoute(GET("/swagger-resources/**"),
                        req -> ok().contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(Collections.singletonList(
                                        new SwaggerResource("default", "/v3/api-docs", "3.0")
                                )));
    }

    static class SwaggerResource {
        private String name;
        private String url;
        private String swaggerVersion;

        public SwaggerResource(String name, String url, String swaggerVersion) {
            this.name = name;
            this.url = url;
            this.swaggerVersion = swaggerVersion;
        }

        public String getName() { return name; }
        public String getUrl() { return url; }
        public String getSwaggerVersion() { return swaggerVersion; }
    }
}