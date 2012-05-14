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

import static javax.xml.XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.schema.SchemaDefinitionFactory;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.util.PrismContextFactory;
import com.evolveum.midpoint.prism.xml.GlobalDynamicNamespacePrefixMapper;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.processor.MidPointSchemaDefinitionFactory;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author semancik
 *
 */
public class MidPointPrismContextFactory implements PrismContextFactory {
	
	private static final File TEST_EXTRA_SCHEMA_DIR = new File("src/test/resources/schema");

	public static final MidPointPrismContextFactory FACTORY = new MidPointPrismContextFactory(TEST_EXTRA_SCHEMA_DIR);
	
	private File extraSchemaDir;
		
	public MidPointPrismContextFactory() {
		this.extraSchemaDir = null;
	}

	public MidPointPrismContextFactory(File extraSchemaDir) {
		this.extraSchemaDir = extraSchemaDir;
	}

	@Override
	public PrismContext createPrismContext() throws SchemaException, FileNotFoundException {
		SchemaRegistry schemaRegistry = createSchemaRegistry();
		PrismContext context = PrismContext.create(schemaRegistry);
		context.setDefinitionFactory(createDefinitionFactory());
		return context;
	}
	
	private SchemaDefinitionFactory createDefinitionFactory() {
		return new MidPointSchemaDefinitionFactory();
	}

	public PrismContext createInitializedPrismContext() throws SchemaException, SAXException, IOException {
		PrismContext context = createPrismContext();
		context.initialize();
		return context;
	}
	
	private SchemaRegistry createSchemaRegistry() throws SchemaException, FileNotFoundException {
		SchemaRegistry schemaRegistry = new SchemaRegistry();
		schemaRegistry.setObjectSchemaNamespace(SchemaConstantsGenerated.NS_COMMON);
		schemaRegistry.setNamespacePrefixMapper(new GlobalDynamicNamespacePrefixMapper());
		registerBuiltinSchemas(schemaRegistry);
        registerExtensionSchemas(schemaRegistry);
		return schemaRegistry;
	}
    
    protected void registerExtensionSchemas(SchemaRegistry schemaRegistry) throws SchemaException, FileNotFoundException {
    	if (extraSchemaDir != null && extraSchemaDir.exists()) {
    		schemaRegistry.registerPrismSchemasFromDirectory(extraSchemaDir);
    	}
    }
	
	private void registerBuiltinSchemas(SchemaRegistry schemaRegistry) throws SchemaException {
		
		schemaRegistry.registerPrismSchemaResource("xml/ns/public/common/common-1.xsd", "c", 
				com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectFactory.class.getPackage());
		
		schemaRegistry.registerPrismSchemaResource("xml/ns/public/prism/types-2.xsd", "t", 
				com.evolveum.prism.xml.ns._public.types_2.ObjectFactory.class.getPackage());
		
		schemaRegistry.registerPrismSchemaResource("xml/ns/public/common/api-types-2.xsd", "apti", 
				com.evolveum.midpoint.xml.ns._public.common.api_types_2.ObjectFactory.class.getPackage());
		
		schemaRegistry.registerPrismSchemaResource("xml/ns/public/prism/query-2.xsd", "q", 
				com.evolveum.prism.xml.ns._public.query_2.ObjectFactory.class.getPackage());
		
		schemaRegistry.registerPrismSchemaResource("xml/ns/public/prism/annotation-2.xsd", "a");
		
		schemaRegistry.registerPrismSchemaResource("xml/ns/public/resource/resource-schema-1.xsd", "r");
		schemaRegistry.registerPrismSchemaResource("xml/ns/public/resource/annotation-2.xsd", "ra");
		
		schemaRegistry.registerPrismSchemaResource("xml/ns/public/resource/capabilities-1.xsd", "cap",
				com.evolveum.midpoint.xml.ns._public.resource.capabilities_1.ObjectFactory.class.getPackage());
		
		schemaRegistry.registerPrismSchemaResource("xml/ns/public/connector/icf-1/connector-schema-1.xsd", "icfc",
				com.evolveum.midpoint.xml.ns._public.connector.icf_1.connector_schema_1.ObjectFactory.class.getPackage());
		
		schemaRegistry.registerPrismSchemaResource("xml/ns/public/connector/icf-1/resource-schema-1.xsd", "icfs",
				com.evolveum.midpoint.xml.ns._public.connector.icf_1.resource_schema_1.ObjectFactory.class.getPackage());
		
		schemaRegistry.registerPrismSchemaResource("xml/ns/public/model/import/extension-2.xsd", "impext");
		
		schemaRegistry.registerSchemaResource("xml/ns/standard/XMLSchema.xsd", "xsd");
		schemaRegistry.registerSchemaResource("xml/ns/standard/xmldsig-core-schema.xsd", "ds");
		schemaRegistry.registerSchemaResource("xml/ns/standard/xenc-schema.xsd", "enc");
		
		schemaRegistry.getNamespacePrefixMapper().registerPrefix(W3C_XML_SCHEMA_INSTANCE_NS_URI, "xsi");
	}
	
	private void setupDebug() {
		DebugUtil.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
	}

}
