/**
 * Copyright (c) 2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.provisioning.ucf.api;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.schema.PrismSchemaImpl;
import com.evolveum.midpoint.schema.util.ConnectorTypeUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorHostType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.XmlSchemaType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * @author semancik
 *
 */
public class UcfUtil {
	
	public static void addConnectorNames(ConnectorType connectorType, String frameworkPrefix, String bundle, String type, String version, ConnectorHostType hostType) {
		StringBuilder connectorName = new StringBuilder();
		connectorName.append(frameworkPrefix).append(" ");
		connectorName.append(type);
		connectorName.append(" v");
		connectorName.append(version);
		StringBuilder displayName = new StringBuilder(StringUtils.substringAfterLast(type, "."));
		if (hostType != null) {
			connectorName.append(" @");
			connectorName.append(hostType.getName());
			displayName.append(" @");
			displayName.append(hostType.getName());
		}
		connectorType.setName(new PolyStringType(connectorName.toString()));
		connectorType.setDisplayName(new PolyStringType(displayName.toString()));
	}

	public static PrismSchema getConnectorSchema(ConnectorType connectorType, PrismContext prismContext) throws SchemaException {
		XmlSchemaType xmlSchema = connectorType.getSchema();
		if (xmlSchema == null) {
			return null;
		}
		Element xsdElement = ObjectTypeUtil.findXsdElement(xmlSchema);
		if (xsdElement == null) {
			return null;
		}
		PrismSchema connectorSchema = PrismSchemaImpl.parse(xsdElement, true, connectorType.toString(), prismContext);
		return connectorSchema;
	}
	
	public static void setConnectorSchema(ConnectorType connectorType, PrismSchema connectorSchema) throws SchemaException {
		Document xsdDoc = connectorSchema.serializeToXsd();
		Element xsdElement = DOMUtil.getFirstChildElement(xsdDoc);
		ConnectorTypeUtil.setConnectorXsdSchema(connectorType, xsdElement);
	}
	
}
