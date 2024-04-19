/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.util.exception.SchemaException;

import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Element;

public class ConnectorSchemaFactory {

    public static @NotNull ConnectorSchemaImpl newConnectorSchema(@NotNull String namespace) {
        return new ConnectorSchemaImpl(namespace);
    }

    public static @NotNull ConnectorSchemaImpl parse(@NotNull Element element, String shortDesc) throws SchemaException {
        return new ConnectorSchemaImpl(element, shortDesc);
    }
}
