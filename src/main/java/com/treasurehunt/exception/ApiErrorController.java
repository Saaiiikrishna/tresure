package com.treasurehunt.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashMap;
import java.util.Map;

/**
 * Central error controller that returns JSON for API paths ("/api/**")
 * and defers to default error handling for non-API paths.
 */
@Controller
public class ApiErrorController implements ErrorController {

    private static final Logger logger = LoggerFactory.getLogger(ApiErrorController.class);

    @RequestMapping("/error")
    public ResponseEntity<?> handleError(HttpServletRequest request) {
        Integer statusCode = (Integer) request.getAttribute("jakarta.servlet.error.status_code");
        String requestUri = (String) request.getAttribute("jakarta.servlet.error.request_uri");
        Throwable exception = (Throwable) request.getAttribute("jakarta.servlet.error.exception");

        int status = statusCode != null ? statusCode : HttpStatus.INTERNAL_SERVER_ERROR.value();
        if (requestUri != null && requestUri.startsWith("/api")) {
            Map<String, Object> body = new HashMap<>();
            body.put("error", status == 404 ? "NOT_FOUND" : "ERROR");
            body.put("status", status);
            body.put("path", requestUri);
            body.put("message", status == 404 ? "Resource not found" : "Unexpected error");
            if (status >= 500) {
                logger.error("API error ({}): {}", status, requestUri, exception);
            } else {
                logger.warn("API error ({}): {}", status, requestUri);
            }
            return ResponseEntity.status(status).body(body);
        }

        // For non-API paths, return status without body to allow client-side handling
        return ResponseEntity.status(status).build();
    }
}

