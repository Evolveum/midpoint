/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.schema.processor.ConnectorSchema;
import com.evolveum.midpoint.schema.processor.ConnectorSchemaFactory;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.XmlSchemaType;
import com.evolveum.prism.xml.ns._public.types_3.SchemaDefinitionType;

/**
 * @author Radovan Semancik
 */
public class ConnectorTypeUtil {

    public static String getConnectorHostTypeOid(ConnectorType connectorType) {
        if (connectorType.getConnectorHostRef() != null) {
            return connectorType.getConnectorHostRef().getOid();
        } else {
            return null;
        }
    }

    public static @Nullable Element getConnectorXsdSchemaElement(ConnectorType connector) {
        XmlSchemaType xmlSchemaType = connector.getSchema();
        if (xmlSchemaType == null) {
            return null;
        }
        return ObjectTypeUtil.findXsdElement(xmlSchemaType);
    }

    public static @NotNull Element getConnectorXsdSchemaElementRequired(ConnectorType connector) throws SchemaException {
        return MiscUtil.requireNonNull(
                getConnectorXsdSchemaElement(connector),
                "No schema in %s", connector);
    }

    public static Element getConnectorXsdSchemaElement(PrismObject<ConnectorType> connector) {
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
    public static @NotNull ConnectorSchema parseConnectorSchema(ConnectorType connector) throws SchemaException {
        return MiscUtil.requireNonNull(
                parseConnectorSchemaIfPresent(connector),
                "No schema in %s", connector);
    }

    public static @Nullable ConnectorSchema parseConnectorSchemaIfPresent(ConnectorType connector) throws SchemaException {
        var schemaElement = getConnectorXsdSchemaElement(connector);
        return schemaElement != null ? ConnectorSchemaFactory.parse(schemaElement, connector.toString()) : null;
    }
}
