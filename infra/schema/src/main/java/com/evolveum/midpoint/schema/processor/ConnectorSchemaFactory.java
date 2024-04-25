/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.prism.impl.schema.SchemaParsingUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorConfigurationType;

import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Element;

public class ConnectorSchemaFactory {

    public static @NotNull ConnectorSchemaImpl newConnectorSchema(@NotNull String namespace) {
        return new ConnectorSchemaImpl(namespace);
    }

    /**
     * Creates the connector schema from provided DOM {@link Element}.
     *
     * Implementation note: Regarding the synchronization, please see the explanation in
     * {@link SchemaParsingUtil#createAndParse(Element, boolean, String, boolean)}.
     */
    public static @NotNull ConnectorSchemaImpl parse(@NotNull Element element, String shortDesc) throws SchemaException {
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (element) {
            var schema = newConnectorSchema(DOMUtil.getSchemaTargetNamespace(element));
            SchemaParsingUtil.parse(schema, element, true, shortDesc, false);
            fixConnectorConfigurationDefinition(schema);
            return schema;
        }
    }

    /**
     * Make sure that the connector configuration definition has a correct compile-time class name and maxOccurs setting:
     *
     * . For the compile-time class: the type is {@link ConnectorSchemaImpl#CONNECTOR_CONFIGURATION_TYPE_LOCAL_NAME}
     * (ConnectorConfigurationType), but the standard schema parser does not know what compile time class to use.
     * So we need to fix it here.
     *
     * . For the maxOccurs, it is currently not being serialized for the top-level schema items. So we must fix that here.
     */
    private static void fixConnectorConfigurationDefinition(ConnectorSchemaImpl schema) throws SchemaException {
        var configurationContainerDefinition = schema.getConnectorConfigurationContainerDefinition();
        configurationContainerDefinition.mutator().setCompileTimeClass(ConnectorConfigurationType.class);
        configurationContainerDefinition.mutator().setMaxOccurs(1);
    }
}
