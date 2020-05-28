/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.rest.impl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotWritableException;
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

    /**
     * Registers content-types for path extension and request parameter usage.
     * Not needed for header-based content negotiation and midPoint typically
     * doesn't use this, but it doesn't hurt and may be handy for download URLs.
     * <p>
     * See <a href="https://www.baeldung.com/spring-mvc-content-negotiation-json-xml">this
     * tutorial for more about content negotiation</a>.
     */
    @Override
    public void configureContentNegotiation(@NotNull ContentNegotiationConfigurer configurer) {
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

    /**
     * All beans above will be first in the converter list and other Spring converters
     * will be available as well.
     * We want to add "catch-all" converter for cases like error output for any (even unsupported)
     * content type.
     */
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(new FallbackConverter());
    }

    private static class FallbackConverter extends AbstractHttpMessageConverter<Object> {
        /**
         * Supports all media types - that's the purpose of this converter.
         */
        protected FallbackConverter() {
            super(MediaType.ALL);
        }

        /**
         * Supports all object types - that's the purpose of this converter.
         */
        @Override
        protected boolean supports(@NotNull Class<?> clazz) {
            return true;
        }

        /**
         * Only for output, this can't read anything.
         */
        @Override
        public boolean canRead(@NotNull Class<?> clazz, @Nullable MediaType mediaType) {
            return false;
        }

        @Override
        protected @NotNull Object readInternal(
                @NotNull Class<?> clazz, @NotNull HttpInputMessage inputMessage) {
            throw new UnsupportedOperationException("FallbackConverter is write-only");
        }

        @Override
        protected void writeInternal(@NotNull Object o, @NotNull HttpOutputMessage outputMessage)
                throws IOException, HttpMessageNotWritableException {
            outputMessage.getBody().write(o.toString().getBytes(StandardCharsets.UTF_8));
        }
    }
}
