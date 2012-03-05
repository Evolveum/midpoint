/**
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.provisioning;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;

import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.provisioning.ucf.impl.ConnectorFactoryIcfImpl;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.XmlSchemaType;

/**
 * @author semancik
 *
 */
public class ProvisioningTestUtil {

	public static void assertConnectorSchemaSanity(ConnectorType conn, PrismContext prismContext) throws SchemaException {
		XmlSchemaType xmlSchemaType = conn.getSchema();
		assertNotNull("xmlSchemaType is null",xmlSchemaType);
		assertFalse("Empty schema",xmlSchemaType.getAny().isEmpty());
		Element xsdElement = ObjectTypeUtil.findXsdElement(xmlSchemaType);
		assertNotNull("No xsd:schema element in xmlSchemaType",xsdElement);
		display("XSD schema of "+conn, DOMUtil.serializeDOMToString(xsdElement));
		// Try to parse the schema
		PrismSchema schema = null;
		try {
			schema = PrismSchema.parse(xsdElement, prismContext);
		} catch (SchemaException e) {
			throw new SchemaException("Error parsing schema of "+conn+": "+e.getMessage(),e);
		}
		assertConnectorSchemaSanity(schema, conn.toString());
	}
	
	public static void assertConnectorSchemaSanity(PrismSchema schema, String connectorDescription) {
		assertNotNull("Cannot parse connector schema of "+connectorDescription,schema);
		assertFalse("Empty connector schema in "+connectorDescription,schema.isEmpty());
		display("Parsed connector schema of "+connectorDescription,schema);
		
		// Local schema namespace is used here.
		PrismContainerDefinition configurationDefinition = 
			schema.findItemDefinition("configuration",PrismContainerDefinition.class);
		assertNotNull("Definition of <configuration> property container not found in connector schema of "+connectorDescription,
				configurationDefinition);
		assertFalse("Empty definition of <configuration> property container in connector schema of "+connectorDescription,
				configurationDefinition.isEmpty());
		
		// ICFC schema is used on other elements
		PrismContainerDefinition configurationPropertiesDefinition = 
			configurationDefinition.findContainerDefinition(ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_CONFIGURATION_PROPERTIES_ELEMENT_QNAME);
		assertNotNull("Definition of <configurationProperties> property container not found in connector schema of "+connectorDescription,
				configurationPropertiesDefinition);
		assertFalse("Empty definition of <configurationProperties> property container in connector schema of "+connectorDescription,
				configurationPropertiesDefinition.isEmpty());
		assertFalse("No definitions in <configurationProperties> in "+connectorDescription, configurationPropertiesDefinition.getDefinitions().isEmpty());

		// TODO: other elements
	}

}
