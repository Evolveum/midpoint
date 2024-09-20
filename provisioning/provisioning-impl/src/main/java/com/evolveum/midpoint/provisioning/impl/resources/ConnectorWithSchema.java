/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resources;

import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.schema.processor.ConnectorSchema;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Combination of a connector object with parsed schema.
 *
 * This is an alternative to attaching the schema data to {@link ConnectorType} {@link PrismObject} by a user data map.
 */
@SuppressWarnings("ClassCanBeRecord")
@Experimental
class ConnectorWithSchema {

    @NotNull private final ConnectorType connector;
    @NotNull private final ConnectorSchema schema;

    ConnectorWithSchema(@NotNull ConnectorType connector, @NotNull ConnectorSchema schema) {
        this.connector = connector;
        this.schema = schema;
    }

    public @NotNull ConnectorType getConnector() {
        return connector;
    }

    public @NotNull PrismSchema getSchema() {
        return schema;
    }

    @NotNull PrismContainerDefinition<ConnectorConfigurationType> getConfigurationContainerDefinition()
            throws SchemaException {
        return schema.getConnectorConfigurationContainerDefinition();
    }

    @Nullable String getConnectorHostOid() {
        ObjectReferenceType ref = connector.getConnectorHostRef();
        return ref != null ? ref.getOid() : null;
    }
}
