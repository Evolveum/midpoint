/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
            MediaType.valueOf("application/*-yaml"), MediaType.valueOf("text/*-yaml"),
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
