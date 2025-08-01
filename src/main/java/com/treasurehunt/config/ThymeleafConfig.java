package com.treasurehunt.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.StringTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;

@Configuration
public class ThymeleafConfig {

    @Bean
    public TemplateEngine stringTemplateEngine() {
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        
        // Add string template resolver for processing content directly
        StringTemplateResolver stringTemplateResolver = new StringTemplateResolver();
        stringTemplateResolver.setTemplateMode(TemplateMode.HTML);
        stringTemplateResolver.setCacheable(false); // Don't cache for preview
        stringTemplateResolver.setOrder(1);
        
        templateEngine.addTemplateResolver(stringTemplateResolver);
        
        return templateEngine;
    }
}
