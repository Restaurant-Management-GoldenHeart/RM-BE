package org.example.goldenheartrestaurant.common.config;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;

import java.nio.charset.StandardCharsets;
import java.util.Set;

@Configuration
public class InvoicePdfTemplateConfig {

    @Bean("invoicePdfTemplateResolver")
    public SpringResourceTemplateResolver invoicePdfTemplateResolver(ApplicationContext applicationContext) {
        SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
        resolver.setApplicationContext(applicationContext);
        resolver.setPrefix("classpath:/templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resolver.setTemplateMode(TemplateMode.XML);
        resolver.setCacheable(false);
        resolver.setCheckExistence(true);
        resolver.setResolvablePatterns(Set.of("billing/*"));
        return resolver;
    }

    @Bean("invoicePdfTemplateEngine")
    public TemplateEngine invoicePdfTemplateEngine(
            @Qualifier("invoicePdfTemplateResolver") SpringResourceTemplateResolver invoicePdfTemplateResolver
    ) {
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(invoicePdfTemplateResolver);
        return engine;
    }
}
