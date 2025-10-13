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

public class MidpointJsonHttpMessageConverter extends MidpointAbstractHttpMessageConverter<Object> {

    public static final MediaType[] MEDIA_TYPES = {
            MediaType.APPLICATION_JSON
    };

    protected MidpointJsonHttpMessageConverter(
            PrismContext prismContext, LocalizationService localizationService) {
        super(prismContext, localizationService, MEDIA_TYPES);
    }

    @Override
    protected PrismSerializer<String> getSerializer() {
        return prismContext.jsonSerializer();
    }

    @Override
    protected PrismParser getParser(InputStream entityStream) {
        return prismContext.parserFor(entityStream).json();
    }
}
