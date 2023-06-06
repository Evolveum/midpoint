/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.util;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.impl.schema.PrismSchemaImpl;
import com.evolveum.midpoint.util.MiscUtil;

import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.XmlSchemaType;
import com.evolveum.prism.xml.ns._public.types_3.SchemaDefinitionType;

/**
 * @author Radovan Semancik
 *
 */
public class ConnectorTypeUtil {

    public static String getConnectorHostTypeOid(ConnectorType connectorType) {
        if (connectorType.getConnectorHostRef() != null) {
            return connectorType.getConnectorHostRef().getOid();
        } else {
            return null;
        }
    }

    public static Element getConnectorXsdSchema(ConnectorType connector) {
        XmlSchemaType xmlSchemaType = connector.getSchema();
        if (xmlSchemaType == null) {
            return null;
        }
        return ObjectTypeUtil.findXsdElement(xmlSchemaType);
    }

    public static Element getConnectorXsdSchema(PrismObject<ConnectorType> connector) {
        PrismContainer<XmlSchemaType> xmlSchema = connector.findContainer(ConnectorType.F_SCHEMA);
        if (xmlSchema == null) {
            return null;
        }
        return ObjectTypeUtil.findXsdElement(xmlSchema);
    }

    public static void setConnectorXsdSchema(ConnectorType connectorType, Element xsdElement) {
        PrismObject<ConnectorType> connector = connectorType.asPrismObject();
        setConnectorXsdSchema(connector, xsdElement);
    }

    public static void setConnectorXsdSchema(PrismObject<ConnectorType> connector, Element xsdElement) {
        PrismContainer<XmlSchemaType> schemaContainer;
        try {
            schemaContainer = connector.findOrCreateContainer(ConnectorType.F_SCHEMA);

            PrismContainerValue<XmlSchemaType> schemaInstance = schemaContainer.getAnyValue();
            if (schemaInstance == null) {
                schemaInstance = schemaContainer.createNewValue();
            }
            PrismProperty<SchemaDefinitionType> definitionProperty = schemaInstance.findOrCreateProperty(XmlSchemaType.F_DEFINITION);
            ObjectTypeUtil.setXsdSchemaDefinition(definitionProperty, xsdElement);
        } catch (SchemaException e) {
            // Should not happen
            throw new IllegalStateException("Internal schema error: "+e.getMessage(),e);
        }

    }

    /**
     * Returns parsed connector schema
     */
    public static PrismSchema parseConnectorSchema(ConnectorType connectorType) throws SchemaException {
        Element connectorSchemaElement = ConnectorTypeUtil.getConnectorXsdSchema(connectorType);
        if (connectorSchemaElement == null) {
            return null;
        }
        PrismSchema connectorSchema =
                PrismSchemaImpl.parse(
                        connectorSchemaElement, true, "schema for " + connectorType, PrismContext.get());
        // Make sure that the config container definition has a correct compile-time class name
        QName configContainerQName = new QName(connectorType.getNamespace(), ResourceType.F_CONNECTOR_CONFIGURATION.getLocalPart());
        PrismContainerDefinition<ConnectorConfigurationType> configurationContainerDefinition =
                connectorSchema.findContainerDefinitionByElementName(configContainerQName);
        configurationContainerDefinition.toMutable().setCompileTimeClass(ConnectorConfigurationType.class);
        return connectorSchema;
    }

    public static @NotNull PrismContainerDefinition<ConnectorConfigurationType> findConfigurationContainerDefinitionRequired(
            ConnectorType connector, PrismSchema connectorSchema) throws SchemaException {
        return MiscUtil.requireNonNull(
                findConfigurationContainerDefinition(connector, connectorSchema),
                () -> "No configuration container definition in schema of " + connector);
    }

    public static PrismContainerDefinition<ConnectorConfigurationType> findConfigurationContainerDefinition(
            ConnectorType connector, PrismSchema connectorSchema) {
        return connectorSchema.findContainerDefinitionByElementName(
                new QName(
                        connector.getNamespace(),
                        ResourceType.F_CONNECTOR_CONFIGURATION.getLocalPart()));
    }
}
