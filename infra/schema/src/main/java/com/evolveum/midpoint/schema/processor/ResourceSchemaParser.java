/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Element;

public class ResourceSchemaParser {

    @NotNull private final Element xsdSchema;
    @NotNull private final String description;

    public ResourceSchemaParser(@NotNull Element xsdSchema, @NotNull String description) {
        this.xsdSchema = xsdSchema;
        this.description = description;
    }

    public static @NotNull ResourceSchema parse(Element xsdSchema, String description) throws SchemaException {
        return new ResourceSchemaParser(xsdSchema, description)
                .parse();
    }

    private ResourceSchema parse() throws SchemaException {
        ResourceSchemaImpl schema = new ResourceSchemaImpl();
        schema.parseThis(xsdSchema, true, description, PrismContext.get());
        return schema;
    }
}
