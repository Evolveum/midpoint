/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorConfigurationType;

/** The schema for connector configuration. */
public interface ConnectorSchema extends PrismSchema {

    /**
     * This is the container that holds the whole configuration. Its content is defined by the connector
     * and the connector framework.
     *
     * E.g., for ConnId, there is a connector-specific part defined by the particular connector
     * (see {@link SchemaConstants#ICF_CONFIGURATION_PROPERTIES_NAME}), and generic parts prescribed
     * by ConnId itself, like `connectorPoolConfiguration` or `timeouts`).
     */
    String CONNECTOR_CONFIGURATION_LOCAL_NAME = "connectorConfiguration";
    String CONNECTOR_CONFIGURATION_TYPE_LOCAL_NAME = "ConfigurationType";

    default @NotNull ItemName getConnectorConfigurationContainerName() {
        return new ItemName(getNamespace(), CONNECTOR_CONFIGURATION_LOCAL_NAME);
    }

    default @NotNull PrismContainerDefinition<ConnectorConfigurationType> getConnectorConfigurationContainerDefinition()
            throws SchemaException {
        var containerName = getConnectorConfigurationContainerName();
        return MiscUtil.requireNonNull(
                findContainerDefinitionByElementName(containerName),
        "No definition for container %s in schema %s", containerName, this);
    }

    String getUsualNamespacePrefix();
}
