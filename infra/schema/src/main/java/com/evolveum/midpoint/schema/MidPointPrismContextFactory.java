/*
 * Copyright (c) 2010-2017 Evolveum
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
package com.evolveum.midpoint.schema;

import javax.xml.XMLConstants;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

import com.evolveum.midpoint.prism.PrismContextImpl;
import com.evolveum.midpoint.prism.schema.SchemaRegistryImpl;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.internals.InternalsConfig;

import org.jetbrains.annotations.NotNull;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.schema.SchemaDefinitionFactory;
import com.evolveum.midpoint.prism.util.PrismContextFactory;
import com.evolveum.midpoint.prism.xml.GlobalDynamicNamespacePrefixMapper;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.processor.MidPointSchemaDefinitionFactory;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
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
		SchemaRegistryImpl schemaRegistry = createSchemaRegistry();
		PrismContextImpl context = PrismContextImpl.create(schemaRegistry);
		context.setDefinitionFactory(createDefinitionFactory());
		context.setDefaultRelation(SchemaConstants.ORG_DEFAULT);
		context.setObjectsElementName(SchemaConstants.C_OBJECTS);
		if (InternalsConfig.isPrismMonitoring()) {
			context.setMonitor(new InternalMonitor());
		}
		context.setParsingMigrator(new MidpointParsingMigrator());
		return context;
	}

	public PrismContext createEmptyPrismContext() throws SchemaException, FileNotFoundException {
		SchemaRegistryImpl schemaRegistry = createSchemaRegistry();
		PrismContextImpl context = PrismContextImpl.createEmptyContext(schemaRegistry);
		context.setDefinitionFactory(createDefinitionFactory());
		context.setDefaultRelation(SchemaConstants.ORG_DEFAULT);
		context.setObjectsElementName(SchemaConstants.C_OBJECTS);
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

	@NotNull
	private SchemaRegistryImpl createSchemaRegistry() throws SchemaException, FileNotFoundException {
		SchemaRegistryImpl schemaRegistry = new SchemaRegistryImpl();
		schemaRegistry.setDefaultNamespace(SchemaConstantsGenerated.NS_COMMON);
		schemaRegistry.setNamespacePrefixMapper(new GlobalDynamicNamespacePrefixMapper());
		registerBuiltinSchemas(schemaRegistry);
        registerExtensionSchemas(schemaRegistry);
		return schemaRegistry;
	}

    protected void registerExtensionSchemas(SchemaRegistryImpl schemaRegistry) throws SchemaException, FileNotFoundException {
    	if (extraSchemaDir != null && extraSchemaDir.exists()) {
    		schemaRegistry.registerPrismSchemasFromDirectory(extraSchemaDir);
    	}
    }

	private void registerBuiltinSchemas(SchemaRegistryImpl schemaRegistry) throws SchemaException {
		// Note: the order of schema registration may affect the way how the schema files are located
		// (whether are pulled from the registry or by using a catalog file).

		// Standard schemas

		schemaRegistry.getNamespacePrefixMapper().registerPrefix("http://www.w3.org/2001/XMLSchema", DOMUtil.NS_W3C_XML_SCHEMA_PREFIX, false);
		schemaRegistry.getNamespacePrefixMapper().registerPrefix(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, "xsi", false);


		// Prism Schemas
		schemaRegistry.registerPrismSchemaResource("xml/ns/public/annotation-3.xsd", "a");

		schemaRegistry.registerPrismSchemaResource("xml/ns/public/types-3.xsd", "t",
				com.evolveum.prism.xml.ns._public.types_3.ObjectFactory.class.getPackage(), true);          // declared by default

		schemaRegistry.registerPrismSchemaResource("xml/ns/public/query-3.xsd", "q",
				com.evolveum.prism.xml.ns._public.query_3.ObjectFactory.class.getPackage(), true);          // declared by default


		// midPoint schemas
		schemaRegistry.registerPrismDefaultSchemaResource("xml/ns/public/common/common-3.xsd", "c",
				com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectFactory.class.getPackage());         // declared by default

		schemaRegistry.registerPrismSchemaResource("xml/ns/public/common/audit-3.xsd", "aud",
				com.evolveum.midpoint.xml.ns._public.common.audit_3.ObjectFactory.class.getPackage());

		schemaRegistry.registerPrismSchemaResource("xml/ns/public/common/api-types-3.xsd", "apti",
				com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectFactory.class.getPackage());

        schemaRegistry.registerPrismSchemasFromWsdlResource("xml/ns/public/model/model-3.wsdl",
                Arrays.asList(com.evolveum.midpoint.xml.ns._public.model.model_3.ObjectFactory.class.getPackage()));

        schemaRegistry.registerPrismSchemasFromWsdlResource("xml/ns/public/report/report-3.wsdl",
                Arrays.asList(com.evolveum.midpoint.xml.ns._public.report.report_3.ObjectFactory.class.getPackage()));

//        schemaRegistry.registerPrismSchemasFromWsdlResource("xml/ns/public/report/report-3.wsdl",
//                Arrays.asList(com.evolveum.midpoint.xml.ns._public.report.report_3.ObjectFactory.class.getPackage()));

		schemaRegistry.registerPrismSchemaResource("xml/ns/public/resource/annotation-3.xsd", "ra");

		schemaRegistry.registerPrismSchemaResource("xml/ns/public/resource/capabilities-3.xsd", "cap",
				com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ObjectFactory.class.getPackage());

		schemaRegistry.registerPrismSchemaResource("xml/ns/public/connector/icf-1/connector-schema-3.xsd", "icfc",
				com.evolveum.midpoint.xml.ns._public.connector.icf_1.connector_schema_3.ObjectFactory.class.getPackage());

		schemaRegistry.registerPrismSchemaResource("xml/ns/public/connector/icf-1/resource-schema-3.xsd", "icfs",
				com.evolveum.midpoint.xml.ns._public.connector.icf_1.resource_schema_3.ObjectFactory.class.getPackage(), true); // declared by default

		schemaRegistry.registerPrismSchemaResource("xml/ns/public/model/extension-3.xsd", "mext");
		schemaRegistry.registerPrismSchemaResource("xml/ns/public/report/extension-3.xsd", "rext");

        schemaRegistry.registerPrismSchemaResource("xml/ns/public/model/scripting/scripting-3.xsd", "s",
                com.evolveum.midpoint.xml.ns._public.model.scripting_3.ObjectFactory.class.getPackage());

        schemaRegistry.registerPrismSchemaResource("xml/ns/public/task/noop-3.xsd", "noop");


		schemaRegistry.registerPrismSchemaResource("xml/ns/public/task/jdbc-ping-3.xsd", "jping");

        schemaRegistry.registerPrismSchemaResource("xml/ns/public/task/extension-3.xsd", "taskext");

        schemaRegistry.registerPrismSchemaResource("xml/ns/public/connector/icf-1/connector-extension-3.xsd", "connext");

        schemaRegistry.registerPrismSchemaResource("xml/ns/public/model/scripting/extension-3.xsd", "se");

        schemaRegistry.getNamespacePrefixMapper().registerPrefix(MidPointConstants.NS_RI, MidPointConstants.PREFIX_NS_RI, false);
        schemaRegistry.getNamespacePrefixMapper().addDeclaredByDefault(MidPointConstants.PREFIX_NS_RI); // declared by default

        schemaRegistry.getNamespacePrefixMapper().registerPrefix(SchemaConstants.NS_ORG, SchemaConstants.PREFIX_NS_ORG, false);
		schemaRegistry.getNamespacePrefixMapper().addDeclaredByDefault(SchemaConstants.PREFIX_NS_ORG); // declared by default
    }

	private void setupDebug() {
		PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
	}

}
