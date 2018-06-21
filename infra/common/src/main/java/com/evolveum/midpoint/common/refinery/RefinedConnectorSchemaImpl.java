/*
 * Copyright (c) 2010-2014 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 * @author semancik
 *
 */
public class RefinedConnectorSchemaImpl extends ConnectorSchemaImpl implements RefinedConnectorSchema {

	private static final String USER_DATA_KEY_PARSED_CONNECTOR_SCHEMA = RefinedConnectorSchemaImpl.class.getName()+".parsedConnectorSchema";

	protected RefinedConnectorSchemaImpl(PrismContext prismContext) {
		super(prismContext);
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
			connector.setUserData(USER_DATA_KEY_PARSED_CONNECTOR_SCHEMA, parsedSchema);
			return parsedSchema;
		}
	}

	public static void setParsedConnectorSchemaConditional(ConnectorType connectorType, ConnectorSchema parsedSchema) {
		if (hasParsedSchema(connectorType)) {
			return;
		}
		PrismObject<ConnectorType> connector = connectorType.asPrismObject();
		connector.setUserData(USER_DATA_KEY_PARSED_CONNECTOR_SCHEMA, parsedSchema);
	}

	public static boolean hasParsedSchema(ConnectorType connectorType) {
		PrismObject<ConnectorType> connector = connectorType.asPrismObject();
		return connector.getUserData(USER_DATA_KEY_PARSED_CONNECTOR_SCHEMA) != null;
	}

	@Override
	public String toString() {
		return "rSchema(ns=" + namespace + ")";
	}

}
