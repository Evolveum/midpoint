/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.boot;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.evolveum.midpoint.web.application.AsyncWebProcessManager;
import com.evolveum.midpoint.web.application.AsyncWebProcessManagerImpl;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.util.validation.MidpointFormValidatorRegistry;

/**
 * Created by Viliam Repan (lazyman).
 */
@Configuration
@Import(com.evolveum.midpoint.rest.impl.RestConfig.class)
@EnableWebMvc
public class WebConfig {

    @Bean
    public MidPointApplication midpointApplication() {
        return new MidPointApplication();
    }

    @Bean
    public MidpointFormValidatorRegistry midpointFormValidatorRegistry() {
        return new MidpointFormValidatorRegistry();
    }

    @Bean
    public AsyncWebProcessManager asyncWebProcessManager() {
        return new AsyncWebProcessManagerImpl();
    }

    @Configuration
    public static class StaticResourceConfiguration implements WebMvcConfigurer {

        private final WebProperties.Resources resourceProperties = new WebProperties.Resources();


        @Value("${midpoint.home}")
        private String midpointHome;

        @Override
        public void addResourceHandlers(ResourceHandlerRegistry registry) {
            Duration cachePeriod = this.resourceProperties.getCache().getPeriod();
            CacheControl cacheControl = this.resourceProperties.getCache()
                    .getCachecontrol().toHttpCacheControl();
            if (!registry.hasMappingForPattern("/static-web/**")) {
                registry
                        .addResourceHandler("/static-web/**")
                        .addResourceLocations("file://" + midpointHome + "/static-web/")
                        .setCachePeriod(getSeconds(cachePeriod))
                        .setCacheControl(cacheControl);
            }
        }

        private Integer getSeconds(Duration cachePeriod) {
            return (cachePeriod != null ? (int) cachePeriod.getSeconds() : null);
        }
    }
}
