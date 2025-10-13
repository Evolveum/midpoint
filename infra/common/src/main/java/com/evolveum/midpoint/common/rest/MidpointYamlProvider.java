/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.common.rest;

import java.io.InputStream;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.ext.Provider;

import com.evolveum.midpoint.prism.PrismParser;
import com.evolveum.midpoint.prism.PrismSerializer;

@Produces({"application/yaml", "application/x-yaml", "text/yaml", "text/x-yaml"})
@Consumes({"application/yaml", "application/x-yaml", "text/yaml", "text/x-yaml"})
@Provider
public class MidpointYamlProvider<T> extends MidpointAbstractProvider<T> {

    @Override
    protected PrismSerializer<String> getSerializer() {
        return prismContext.yamlSerializer();
    }

    @Override
    protected PrismParser getParser(InputStream entityStream) {
        return prismContext.parserFor(entityStream).yaml();
    }

}
