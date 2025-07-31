package com.treasurehunt.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration
 * Configures static resources and view controllers
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Configure static resource handlers
     * @param registry Resource handler registry
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Static resources
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/")
                .setCachePeriod(3600);

        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/")
                .setCachePeriod(3600);

        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/")
                .setCachePeriod(86400);

        registry.addResourceHandler("/favicon.ico")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(86400);

        // Uploaded files (if serving directly - for development only)
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/")
                .setCachePeriod(3600);
    }

    /**
     * Configure view controllers for simple pages
     * @param registry View controller registry
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Admin login page
        registry.addViewController("/admin/login").setViewName("admin/login");
        
        // Error pages
        registry.addViewController("/error").setViewName("error");
        registry.addViewController("/403").setViewName("error/403");
        registry.addViewController("/404").setViewName("error/404");
        registry.addViewController("/500").setViewName("error/500");
    }
}
