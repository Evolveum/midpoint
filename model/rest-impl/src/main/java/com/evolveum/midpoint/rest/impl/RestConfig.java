/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.rest.impl;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.prism.PrismContext;

/**
 * Spring configuration for MVC-based REST service. TODO: experimental as of early 2020
 * This prepares needed XML/JSON/YAML message converters.
 * It also drives the package scan for REST controllers under this package.
 */
@Configuration
@ComponentScan
public class RestConfig
        implements WebMvcConfigurer {

    // TODO probably @Override addArgumentResolvers for @Converter processing?

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        for (MediaType mediaType : MidpointYamlHttpMessageConverter.MEDIA_TYPES) {
            configurer.mediaType(mediaType.getSubtype(), mediaType);
        }
        configurer.defaultContentType(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML);
    }

    @Bean
    public MidpointYamlHttpMessageConverter yamlConverter(
            PrismContext prismContext, LocalizationService localizationService) {
        return new MidpointYamlHttpMessageConverter(prismContext, localizationService);
    }

    @Bean
    public MidpointXmlHttpMessageConverter xmlConverter(
            PrismContext prismContext, LocalizationService localizationService) {
        return new MidpointXmlHttpMessageConverter(prismContext, localizationService);
    }

    @Bean
    public MidpointJsonHttpMessageConverter jsonConverter(
            PrismContext prismContext, LocalizationService localizationService) {
        return new MidpointJsonHttpMessageConverter(prismContext, localizationService);
    }
}
