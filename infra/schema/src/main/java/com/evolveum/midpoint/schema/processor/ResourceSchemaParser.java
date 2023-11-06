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

/** TODO decide on the fate of this class. */
public class ResourceSchemaParser {

    public static @NotNull ResourceSchema parse(Element xsdSchema, String description) throws SchemaException {
        ResourceSchemaImpl schema = new ResourceSchemaImpl();
        schema.parseThis(xsdSchema, true, description, PrismContext.get());
        return schema;
    }
}
