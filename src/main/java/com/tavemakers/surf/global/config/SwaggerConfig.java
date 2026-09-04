package com.tavemakers.surf.global.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @io.swagger.v3.oas.annotations.info.Info(title = "Surf API", version = "v1"),
        security = @SecurityRequirement(name = "BearerAuth"),
        servers = {
                @Server(url = "https://tavesurf.site", description = "dev 배포 서버"),
                @Server(url = "http://localhost:8080", description = "로컬 서버"),
                @Server(url = "http://3.34.73.43:8080", description = "테스트 서버")
        }
)
public class SwaggerConfig {
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .addSecurityItem(new io.swagger.v3.oas.models.security.SecurityRequirement().addList("BearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("BearerAuth",
                                new SecurityScheme()
                                        .name("Authorization")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                        )
                );
    }
}