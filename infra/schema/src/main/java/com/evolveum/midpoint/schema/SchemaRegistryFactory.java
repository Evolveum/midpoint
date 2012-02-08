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
package com.evolveum.midpoint.schema;

import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;

import com.evolveum.midpoint.prism.SchemaRegistry;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.namespace.MidPointNamespacePrefixMapper;
import com.evolveum.midpoint.schema.namespace.PrefixMapper;
import com.evolveum.midpoint.util.DebugUtil;

/**
 * @author semancik
 *
 */
public class SchemaRegistryFactory {
	
	public SchemaRegistry createSchemaRegistry() throws SchemaException {
		SchemaRegistry schemaRegistry = new SchemaRegistry();
		registerBuiltinSchemas(schemaRegistry);
		schemaRegistry.setObjectSchemaNamespace(SchemaConstants.NS_COMMON);
		schemaRegistry.setNamespacePrefixMapper(new PrefixMapper());
		return schemaRegistry;
	}
	
	private void registerBuiltinSchemas(SchemaRegistry schemaRegistry) throws SchemaException {
		String prefix;
		prefix = MidPointNamespacePrefixMapper.getPreferredPrefix(SchemaConstants.NS_COMMON);
		schemaRegistry.registerMidPointSchemaResource("xml/ns/public/common/common-1.xsd",prefix);
		prefix = MidPointNamespacePrefixMapper.getPreferredPrefix(SchemaConstants.NS_ANNOTATION);
		schemaRegistry.registerMidPointSchemaResource("xml/ns/public/common/annotation-1.xsd",prefix);
		prefix = MidPointNamespacePrefixMapper.getPreferredPrefix(SchemaConstants.NS_RESOURCE);
		schemaRegistry.registerMidPointSchemaResource("xml/ns/public/resource/resource-schema-1.xsd",prefix);
		prefix = MidPointNamespacePrefixMapper.getPreferredPrefix(SchemaConstants.NS_CAPABILITIES);
		schemaRegistry.registerMidPointSchemaResource("xml/ns/public/resource/capabilities-1.xsd",prefix);
		prefix = MidPointNamespacePrefixMapper.getPreferredPrefix(SchemaConstants.NS_ICF_CONFIGURATION);
		schemaRegistry.registerMidPointSchemaResource("xml/ns/public/connector/icf-1/connector-schema-1.xsd",prefix);
		prefix = MidPointNamespacePrefixMapper.getPreferredPrefix(SchemaConstants.NS_ICF_SCHEMA);
		schemaRegistry.registerMidPointSchemaResource("xml/ns/public/connector/icf-1/resource-schema-1.xsd",prefix);
		prefix = MidPointNamespacePrefixMapper.getPreferredPrefix(W3C_XML_SCHEMA_NS_URI);
		schemaRegistry.registerSchemaResource("xml/ns/standard/XMLSchema.xsd",prefix);
	}

	private void setupDebug() {
		DebugUtil.setDefaultNamespacePrefix(SchemaConstants.NS_MIDPOINT_PUBLIC_PREFIX);
	}

}
