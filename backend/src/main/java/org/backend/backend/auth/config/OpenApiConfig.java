package org.backend.backend.auth.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger documentation configuration.
 *
 * Access the UI at: http://localhost:8080/swagger-ui/index.html
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("Starter Template API")
                        .description("""
                                Production-ready Spring Boot authentication backend.
                                
                                **Authentication methods:**
                                - Email + password (JWT)
                                - Google OAuth2 social login
                                - GitHub OAuth2 social login
                                
                                **Role hierarchy:**
                                - `ROLE_USER`   — standard user
                                - `ROLE_VENDOR` — vendor + all USER permissions
                                - `ROLE_ADMIN`  — full access
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Backend Team")
                                .email("rapoluvenky8@gmail.com")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste the JWT access token obtained from /api/auth/login")));
    }
}
