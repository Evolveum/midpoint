/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.rest.impl;

import java.io.InputStream;

import org.springframework.http.MediaType;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismParser;
import com.evolveum.midpoint.prism.PrismSerializer;

public class MidpointYamlHttpMessageConverter extends MidpointAbstractHttpMessageConverter<Object> {

    public static final MediaType[] MEDIA_TYPES = {
            MediaType.valueOf("application/yaml"), MediaType.valueOf("text/yaml"),
            MediaType.valueOf("application/yml"), MediaType.valueOf("text/yml"),
            MediaType.valueOf("application/*+yaml"), MediaType.valueOf("text/*+yaml"),
            MediaType.valueOf("application/*.yaml"), MediaType.valueOf("text/*.yaml")
    };

    protected MidpointYamlHttpMessageConverter(
            PrismContext prismContext, LocalizationService localizationService) {
        super(prismContext, localizationService, MEDIA_TYPES);
    }

    @Override
    protected PrismSerializer<String> getSerializer() {
        return prismContext.yamlSerializer();
    }

    @Override
    protected PrismParser getParser(InputStream entityStream) {
        return prismContext.parserFor(entityStream).yaml();
    }
}
