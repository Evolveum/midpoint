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

public class MidpointXmlHttpMessageConverter extends MidpointAbstractHttpMessageConverter<Object> {

    public static final MediaType[] MEDIA_TYPES = {
            MediaType.APPLICATION_XML, MediaType.TEXT_XML, MediaType.valueOf("application/*+xml")
    };

    protected MidpointXmlHttpMessageConverter(
            PrismContext prismContext, LocalizationService localizationService) {
        super(prismContext, localizationService, MEDIA_TYPES);
    }

    @Override
    protected PrismSerializer<String> getSerializer() {
        return prismContext.xmlSerializer();
    }

    @Override
    protected PrismParser getParser(InputStream entityStream) {
        return prismContext.parserFor(entityStream).xml();
    }
}
