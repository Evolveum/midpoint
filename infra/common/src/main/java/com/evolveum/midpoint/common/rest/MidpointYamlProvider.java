/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.rest;

import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.ext.Provider;

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
