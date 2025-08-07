package com.treasurehunt.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI (Swagger) configuration for API documentation
 * Provides comprehensive API documentation with security schemes
 */
@Configuration
public class OpenApiConfig {

    @Value("${app.version:1.0.0}")
    private String appVersion;

    @Value("${app.name:Treasure Hunt Application}")
    private String appName;

    @Value("${app.description:A comprehensive treasure hunt registration and management system}")
    private String appDescription;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @Value("${server.port:8080}")
    private String serverPort;

    /**
     * Configure OpenAPI documentation
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(apiInfo())
            .servers(List.of(
                new Server()
                    .url("http://localhost:" + serverPort + contextPath)
                    .description("Development Server"),
                new Server()
                    .url("https://api.treasurehunt.com" + contextPath)
                    .description("Production Server")
            ))
            .addSecurityItem(new SecurityRequirement().addList("basicAuth"))
            .addSecurityItem(new SecurityRequirement().addList("sessionAuth"))
            .components(new io.swagger.v3.oas.models.Components()
                .addSecuritySchemes("basicAuth", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("basic")
                    .description("Basic Authentication for Admin endpoints"))
                .addSecuritySchemes("sessionAuth", new SecurityScheme()
                    .type(SecurityScheme.Type.APIKEY)
                    .in(SecurityScheme.In.COOKIE)
                    .name("JSESSIONID")
                    .description("Session-based authentication"))
            );
    }

    /**
     * API information
     */
    private Info apiInfo() {
        return new Info()
            .title(appName + " API")
            .description(appDescription + "\n\n" + getApiDescription())
            .version(appVersion)
            .contact(new Contact()
                .name("Treasure Hunt Team")
                .email("support@treasurehunt.com")
                .url("https://treasurehunt.com/contact"))
            .license(new License()
                .name("MIT License")
                .url("https://opensource.org/licenses/MIT"));
    }

    /**
     * Detailed API description
     */
    private String getApiDescription() {
        return """
            ## Overview
            The Treasure Hunt API provides comprehensive endpoints for managing treasure hunt registrations, 
            plans, and administrative operations.
            
            ## Features
            - **Registration Management**: Individual and team registrations
            - **Plan Management**: Create and manage treasure hunt plans
            - **File Upload**: Secure document and image upload
            - **Admin Operations**: Administrative functions and monitoring
            - **Email Notifications**: Automated email communications
            
            ## Authentication
            The API uses two authentication methods:
            - **Basic Authentication**: For admin endpoints
            - **Session Authentication**: For user-specific operations
            
            ## Rate Limiting
            API requests are rate-limited to ensure fair usage:
            - Public endpoints: 100 requests per minute
            - Authenticated endpoints: 1000 requests per minute
            - Admin endpoints: 500 requests per minute
            
            ## Error Handling
            All API responses follow a consistent error format:
            ```json
            {
              "error": "ERROR_CODE",
              "message": "Human readable error message",
              "status": 400,
              "timestamp": "2024-01-01T12:00:00",
              "path": "/api/endpoint",
              "details": {}
            }
            ```
            
            ## File Upload
            File uploads support the following formats:
            - **Documents**: PDF, JPG, JPEG, PNG (max 5MB)
            - **Images**: JPG, JPEG, PNG, WEBP (max 2MB)
            
            ## Pagination
            List endpoints support pagination with the following parameters:
            - `page`: Page number (0-based, default: 0)
            - `size`: Page size (default: 20, max: 100)
            - `sort`: Sort criteria (e.g., "name,asc" or "createdDate,desc")
            
            ## Status Codes
            - `200 OK`: Successful operation
            - `201 Created`: Resource created successfully
            - `400 Bad Request`: Invalid request data
            - `401 Unauthorized`: Authentication required
            - `403 Forbidden`: Insufficient permissions
            - `404 Not Found`: Resource not found
            - `409 Conflict`: Resource conflict (e.g., duplicate email)
            - `413 Payload Too Large`: File size exceeds limit
            - `422 Unprocessable Entity`: Validation errors
            - `500 Internal Server Error`: Server error
            
            ## API Versioning
            The API uses semantic versioning. Current version: """ + appVersion + """
            
            ## Support
            For API support, please contact: support@treasurehunt.com
            """;
    }

    /**
     * Custom API group configuration for different endpoint categories
     */
    @Bean
    public io.swagger.v3.oas.models.tags.Tag publicApiTag() {
        return new io.swagger.v3.oas.models.tags.Tag()
            .name("Public API")
            .description("Publicly accessible endpoints for treasure hunt plans and registration");
    }

    @Bean
    public io.swagger.v3.oas.models.tags.Tag registrationApiTag() {
        return new io.swagger.v3.oas.models.tags.Tag()
            .name("Registration API")
            .description("Endpoints for managing user registrations");
    }

    @Bean
    public io.swagger.v3.oas.models.tags.Tag adminApiTag() {
        return new io.swagger.v3.oas.models.tags.Tag()
            .name("Admin API")
            .description("Administrative endpoints requiring authentication");
    }

    @Bean
    public io.swagger.v3.oas.models.tags.Tag fileApiTag() {
        return new io.swagger.v3.oas.models.tags.Tag()
            .name("File API")
            .description("Endpoints for file upload and management");
    }

    @Bean
    public io.swagger.v3.oas.models.tags.Tag monitoringApiTag() {
        return new io.swagger.v3.oas.models.tags.Tag()
            .name("Monitoring API")
            .description("System monitoring and health check endpoints");
    }

    /**
     * API documentation customization
     */
    public static class ApiDocumentationCustomizer {
        
        /**
         * Common response examples
         */
        public static final String SUCCESS_RESPONSE_EXAMPLE = """
            {
              "success": true,
              "message": "Operation completed successfully",
              "data": {}
            }
            """;
        
        public static final String ERROR_RESPONSE_EXAMPLE = """
            {
              "error": "VALIDATION_ERROR",
              "message": "Request validation failed",
              "status": 400,
              "timestamp": "2024-01-01T12:00:00",
              "path": "/api/register/individual",
              "details": {
                "fieldErrors": [
                  "email: must be a valid email address",
                  "age: must be at least 18"
                ]
              }
            }
            """;
        
        public static final String REGISTRATION_RESPONSE_EXAMPLE = """
            {
              "id": 123,
              "applicationId": "TH2024-001-123",
              "fullName": "John Doe",
              "email": "john.doe@example.com",
              "phoneNumber": "1234567890",
              "age": 25,
              "status": "PENDING",
              "registrationDate": "2024-01-01T12:00:00",
              "plan": {
                "id": 1,
                "name": "City Adventure Hunt",
                "priceInr": 1500.00
              }
            }
            """;
        
        public static final String PLAN_RESPONSE_EXAMPLE = """
            {
              "id": 1,
              "name": "City Adventure Hunt",
              "description": "An exciting treasure hunt through the city center",
              "durationHours": 3,
              "maxParticipants": 6,
              "availableSlots": 50,
              "priceInr": 1500.00,
              "difficultyLevel": "MEDIUM",
              "status": "ACTIVE",
              "isFeatured": true,
              "createdDate": "2024-01-01T10:00:00"
            }
            """;
    }
}
