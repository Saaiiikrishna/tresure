package com.treasurehunt.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Production security configuration
 * Adds additional security headers and protections for production deployment
 */
@Configuration
@Profile("production")
public class ProductionSecurityConfig {

    /**
     * Security headers filter for production
     */
    @Bean
    public FilterRegistrationBean<SecurityHeadersFilter> securityHeadersFilter() {
        FilterRegistrationBean<SecurityHeadersFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new SecurityHeadersFilter());
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registrationBean;
    }

    /**
     * Custom filter to add security headers
     */
    public static class SecurityHeadersFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                      FilterChain filterChain) throws ServletException, IOException {
            
            // Security Headers for Production
            response.setHeader("X-Content-Type-Options", "nosniff");
            response.setHeader("X-Frame-Options", "DENY");
            response.setHeader("X-XSS-Protection", "1; mode=block");
            response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
            response.setHeader("Permissions-Policy", "geolocation=(), microphone=(), camera=()");
            
            // Content Security Policy
            response.setHeader("Content-Security-Policy", 
                "default-src 'self'; " +
                "script-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://cdnjs.cloudflare.com; " +
                "style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://cdnjs.cloudflare.com; " +
                "img-src 'self' data: https:; " +
                "font-src 'self' https://cdn.jsdelivr.net https://cdnjs.cloudflare.com; " +
                "connect-src 'self'; " +
                "frame-src https://www.youtube.com https://youtube.com; " +
                "object-src 'none'; " +
                "base-uri 'self';"
            );
            
            // Cache Control for sensitive pages
            String requestURI = request.getRequestURI();
            if (requestURI.contains("/admin") || requestURI.contains("/api")) {
                response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
                response.setHeader("Pragma", "no-cache");
                response.setHeader("Expires", "0");
            }
            
            // Remove server information
            response.setHeader("Server", "");
            
            filterChain.doFilter(request, response);
        }
    }

    /**
     * Request logging filter for production monitoring
     */
    @Bean
    public FilterRegistrationBean<RequestLoggingFilter> requestLoggingFilter() {
        FilterRegistrationBean<RequestLoggingFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new RequestLoggingFilter());
        registrationBean.addUrlPatterns("/api/*", "/admin/*");
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        return registrationBean;
    }

    /**
     * Custom filter for request logging
     */
    public static class RequestLoggingFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                      FilterChain filterChain) throws ServletException, IOException {
            
            long startTime = System.currentTimeMillis();
            String method = request.getMethod();
            String uri = request.getRequestURI();
            String remoteAddr = getClientIpAddress(request);
            
            try {
                filterChain.doFilter(request, response);
            } finally {
                long duration = System.currentTimeMillis() - startTime;
                int status = response.getStatus();
                
                // Log suspicious activity
                if (status >= 400 || duration > 5000) {
                    logger.warn("Request: {} {} from {} - Status: {} - Duration: {}ms", 
                              method, uri, remoteAddr, status, duration);
                }
                
                // Log all admin and API requests
                if (uri.contains("/admin") || uri.contains("/api")) {
                    logger.info("API Request: {} {} from {} - Status: {} - Duration: {}ms", 
                              method, uri, remoteAddr, status, duration);
                }
            }
        }
        
        private String getClientIpAddress(HttpServletRequest request) {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                return xForwardedFor.split(",")[0].trim();
            }
            
            String xRealIp = request.getHeader("X-Real-IP");
            if (xRealIp != null && !xRealIp.isEmpty()) {
                return xRealIp;
            }
            
            return request.getRemoteAddr();
        }
        
        private static final Logger logger =
            LoggerFactory.getLogger(RequestLoggingFilter.class);
    }

    /**
     * Rate limiting filter for production
     */
    @Bean
    public FilterRegistrationBean<RateLimitingFilter> rateLimitingFilter() {
        FilterRegistrationBean<RateLimitingFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new RateLimitingFilter());
        registrationBean.addUrlPatterns("/api/register", "/api/contact");
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE + 2);
        return registrationBean;
    }

    /**
     * Simple rate limiting filter
     */
    public static class RateLimitingFilter extends OncePerRequestFilter {
        
        private static final java.util.Map<String, java.util.concurrent.atomic.AtomicInteger> requestCounts = 
            new java.util.concurrent.ConcurrentHashMap<>();
        private static final java.util.Map<String, Long> lastResetTime = 
            new java.util.concurrent.ConcurrentHashMap<>();
        
        private static final int MAX_REQUESTS_PER_MINUTE = 10;
        private static final long RESET_INTERVAL = 60000; // 1 minute
        
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                      FilterChain filterChain) throws ServletException, IOException {
            
            String clientIp = getClientIpAddress(request);
            long currentTime = System.currentTimeMillis();
            
            // Reset counter if interval has passed
            Long lastReset = lastResetTime.get(clientIp);
            if (lastReset == null || (currentTime - lastReset) > RESET_INTERVAL) {
                requestCounts.put(clientIp, new java.util.concurrent.atomic.AtomicInteger(0));
                lastResetTime.put(clientIp, currentTime);
            }
            
            // Check rate limit
            java.util.concurrent.atomic.AtomicInteger count = requestCounts.get(clientIp);
            if (count != null && count.incrementAndGet() > MAX_REQUESTS_PER_MINUTE) {
                response.setStatus(429); // HTTP 429 Too Many Requests
                response.getWriter().write("{\"error\":\"Rate limit exceeded. Please try again later.\"}");
                response.setContentType("application/json");
                
                logger.warn("Rate limit exceeded for IP: {} on URI: {}", clientIp, request.getRequestURI());
                return;
            }
            
            filterChain.doFilter(request, response);
        }
        
        private String getClientIpAddress(HttpServletRequest request) {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                return xForwardedFor.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        }
        
        private static final Logger logger =
            LoggerFactory.getLogger(RateLimitingFilter.class);
    }
}
