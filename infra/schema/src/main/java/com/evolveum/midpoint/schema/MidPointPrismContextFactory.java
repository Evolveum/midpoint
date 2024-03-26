/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import javax.xml.XMLConstants;

import org.jetbrains.annotations.NotNull;
import org.xml.sax.SAXException;

import com.evolveum.axiom.lang.antlr.AxiomModelStatementSource;
import com.evolveum.axiom.lang.spi.AxiomSyntaxException;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.impl.PrismContextImpl;
import com.evolveum.midpoint.prism.impl.schema.SchemaRegistryImpl;
import com.evolveum.midpoint.prism.impl.schema.axiom.AxiomEnabledSchemaRegistry;
import com.evolveum.midpoint.prism.impl.xml.GlobalDynamicNamespacePrefixMapper;
import com.evolveum.midpoint.prism.util.PrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.metadata.MidpointProvenanceEquivalenceStrategy;
import com.evolveum.midpoint.schema.metadata.MidpointValueMetadataFactory;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExtensionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValueMetadataType;
import com.evolveum.midpoint.xml.ns._public.model.model_3.ObjectFactory;

/**
 * @author semancik
 */
public class MidPointPrismContextFactory implements PrismContextFactory {

    private static final File TEST_EXTRA_SCHEMA_DIR = new File("src/test/resources/schema");

    public static final MidPointPrismContextFactory FACTORY = new MidPointPrismContextFactory(TEST_EXTRA_SCHEMA_DIR);

    private final File extraSchemaDir;

    public MidPointPrismContextFactory() {
        this.extraSchemaDir = null;
    }

    public MidPointPrismContextFactory(File extraSchemaDir) {
        this.extraSchemaDir = extraSchemaDir;
    }

    @Override
    public PrismContext createPrismContext() throws SchemaException, IOException {
        SchemaRegistryImpl schemaRegistry = createSchemaRegistry();
        PrismContextImpl context = PrismContextImpl.create(schemaRegistry);
        context.setDefaultRelation(SchemaConstants.ORG_DEFAULT);
        context.setDefaultReferenceTargetType(SchemaConstants.C_OBJECT_TYPE);
        context.setObjectsElementName(SchemaConstants.C_OBJECTS);
        context.setDefaultReferenceTypeName(ObjectReferenceType.COMPLEX_TYPE);
        context.setExtensionContainerTypeName(ExtensionType.COMPLEX_TYPE);
        if (InternalsConfig.isPrismMonitoring()) {
            context.setMonitor(new InternalMonitor());
        }
        context.setParsingMigrator(new MidpointParsingMigrator());
        context.setValueMetadataFactory(new MidpointValueMetadataFactory(context));
        context.setProvenanceEquivalenceStrategy(MidpointProvenanceEquivalenceStrategy.INSTANCE);
        context.registerQueryExpressionFactory(new PrismQueryExpressionSupport());
        return context;
    }

    public PrismContext createInitializedPrismContext() throws SchemaException, SAXException, IOException {
        PrismContext context = createPrismContext();
        context.initialize();
        return context;
    }

    @NotNull
    private SchemaRegistryImpl createSchemaRegistry() throws SchemaException, IOException {
        SchemaRegistryImpl schemaRegistry = new AxiomEnabledSchemaRegistry();
        schemaRegistry.setDefaultNamespace(SchemaConstantsGenerated.NS_COMMON);
        schemaRegistry.setNamespacePrefixMapper(new GlobalDynamicNamespacePrefixMapper());
        registerBuiltinSchemas(schemaRegistry);
        registerExtensionSchemas(schemaRegistry);
        registerAxiomSchemas(schemaRegistry);
        schemaRegistry.setValueMetadataTypeName(ValueMetadataType.COMPLEX_TYPE);
        return schemaRegistry;
    }

    private void registerAxiomSchemas(SchemaRegistryImpl schemaRegistry) {
        if (schemaRegistry instanceof AxiomEnabledSchemaRegistry axiomRegistry) {
            AxiomModelStatementSource commonMetadata;
            try {
                commonMetadata = AxiomModelStatementSource.fromResource("xml/ns/public/common/common-metadata-3.axiom");
            } catch (AxiomSyntaxException | IOException e) {
               throw new RuntimeException(e);
            }
            axiomRegistry.addAxiomSource(commonMetadata);
        }
    }

    protected void registerExtensionSchemas(SchemaRegistryImpl schemaRegistry) throws SchemaException, IOException {
        if (extraSchemaDir != null && extraSchemaDir.exists()) {
            schemaRegistry.registerPrismSchemasFromDirectory(extraSchemaDir);
        }
    }

    private void registerBuiltinSchemas(SchemaRegistryImpl schemaRegistry) throws SchemaException {
        // Note: the order of schema registration may affect the way how the schema files are located
        // (whether are pulled from the registry or by using a catalog file).

        // Standard schemas

        schemaRegistry.registerStaticNamespace(XMLConstants.W3C_XML_SCHEMA_NS_URI, DOMUtil.NS_W3C_XML_SCHEMA_PREFIX, false);
        schemaRegistry.registerStaticNamespace("http://www.w3.org/2001/XMLSchema-instance", "xsi", false);

        // MID-6983: XSI Namespace is emmited by default
        schemaRegistry.getNamespacePrefixMapper().addDeclaredByDefault("xsi");

        // Prism Schemas
        schemaRegistry.registerPrismSchemaResource("xml/ns/public/annotation-3.xsd", "a");

        schemaRegistry.registerPrismSchemaResource("xml/ns/public/types-3.xsd", "t",
                com.evolveum.prism.xml.ns._public.types_3.ObjectFactory.class.getPackage(), true);          // declared by default

        schemaRegistry.registerPrismSchemaResource("xml/ns/public/query-3.xsd", "q",
                com.evolveum.prism.xml.ns._public.query_3.ObjectFactory.class.getPackage(), true);          // declared by default


        // midPoint schemas
        schemaRegistry.registerPrismDefaultSchemaResource("xml/ns/public/common/common-3.xsd", "c",
                com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectFactory.class.getPackage());         // declared by default

        // FIXME: MID-6845: audit.xsd recommend 'audit' prefix
        schemaRegistry.registerPrismSchemaResource("xml/ns/public/common/audit-3.xsd", "aud",
                com.evolveum.midpoint.xml.ns._public.common.audit_3.ObjectFactory.class.getPackage());

        // FIXME: MID-6845: audit.xsd recommend 'apti' prefix
        schemaRegistry.registerPrismSchemaResource("xml/ns/public/common/api-types-3.xsd", "apti",
                com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectFactory.class.getPackage());

        schemaRegistry.registerPrismSchemasFromWsdlResource("xml/ns/public/model/model-3.wsdl",
                Collections.singletonList(ObjectFactory.class.getPackage()));

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

        schemaRegistry.registerStaticNamespace(MidPointConstants.NS_RI, MidPointConstants.PREFIX_NS_RI, false);
        schemaRegistry.getNamespacePrefixMapper().addDeclaredByDefault(MidPointConstants.PREFIX_NS_RI); // declared by default

        schemaRegistry.registerStaticNamespace(SchemaConstants.NS_ORG, SchemaConstants.PREFIX_NS_ORG, false);
        schemaRegistry.getNamespacePrefixMapper().addDeclaredByDefault(SchemaConstants.PREFIX_NS_ORG); // declared by default
    }
}
