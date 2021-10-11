/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.common.refinery;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.processor.ConnectorSchema;
import com.evolveum.midpoint.schema.processor.ConnectorSchemaImpl;
import com.evolveum.midpoint.schema.util.ConnectorTypeUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;

import org.w3c.dom.Element;

/**
 * TODO Think about the purpose and future of this class.
 *
 * @author semancik
 */
public class RefinedConnectorSchemaImpl extends ConnectorSchemaImpl implements RefinedConnectorSchema {

    private static final String USER_DATA_KEY_PARSED_CONNECTOR_SCHEMA = RefinedConnectorSchemaImpl.class.getName()+".parsedConnectorSchema";

    public RefinedConnectorSchemaImpl(String namespace, PrismContext prismContext) {
        super(namespace, prismContext);
    }

    public static ConnectorSchema getConnectorSchema(ConnectorType connectorType, PrismContext prismContext) throws SchemaException {
        PrismObject<ConnectorType> connector = connectorType.asPrismObject();
        return getConnectorSchema(connector, prismContext);
    }

    public static ConnectorSchema getConnectorSchema(PrismObject<ConnectorType> connector, PrismContext prismContext) throws SchemaException {
        Element connectorXsdSchema = ConnectorTypeUtil.getConnectorXsdSchema(connector);
        if (connectorXsdSchema == null) {
            return null;
        }
        Object userDataEntry = connector.getUserData(USER_DATA_KEY_PARSED_CONNECTOR_SCHEMA);
        if (userDataEntry != null) {
            if (userDataEntry instanceof ConnectorSchema) {
                return (ConnectorSchema)userDataEntry;
            } else {
                throw new IllegalStateException("Expected ConnectorSchema under user data key "+
                        USER_DATA_KEY_PARSED_CONNECTOR_SCHEMA + "in "+connector+", but got "+userDataEntry.getClass());
            }
        } else {
            //InternalMonitor.recordConnectorSchemaParse();
            ConnectorSchemaImpl parsedSchema = ConnectorSchemaImpl.parse(connectorXsdSchema, "connector schema of "+connector, prismContext);
            if (parsedSchema == null) {
                throw new IllegalStateException("Parsed schema is null: most likely an internall error");
            }
            parsedSchema.setUsualNamespacePrefix(ConnectorSchemaImpl.retrieveUsualNamespacePrefix(connector.asObjectable()));
            // TODO What if connector is immutable?
            connector.setUserData(USER_DATA_KEY_PARSED_CONNECTOR_SCHEMA, parsedSchema);
            return parsedSchema;
        }
    }

    public static void setParsedConnectorSchemaConditional(ConnectorType connectorType, ConnectorSchema parsedSchema) {
        if (hasParsedSchema(connectorType)) {
            return;
        }
        PrismObject<ConnectorType> connector = connectorType.asPrismObject();
        // TODO What if connector is immutable?
        connector.setUserData(USER_DATA_KEY_PARSED_CONNECTOR_SCHEMA, parsedSchema);
    }

    public static boolean hasParsedSchema(ConnectorType connectorType) {
        PrismObject<ConnectorType> connector = connectorType.asPrismObject();
        return connector.getUserData(USER_DATA_KEY_PARSED_CONNECTOR_SCHEMA) != null;
    }

    @Override
    public String toString() {
        return "rSchema(ns=" + getNamespace() + ")";
    }

}
