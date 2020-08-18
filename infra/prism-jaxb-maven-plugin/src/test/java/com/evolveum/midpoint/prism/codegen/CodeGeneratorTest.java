package com.evolveum.midpoint.prism.codegen;

import java.io.File;
import java.io.IOException;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import com.evolveum.axiom.lang.antlr.AxiomModelStatementSource;
import com.evolveum.axiom.lang.spi.AxiomSyntaxException;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.impl.PrismContextImpl;
import com.evolveum.midpoint.prism.impl.schema.SchemaDefinitionFactory;
import com.evolveum.midpoint.prism.impl.schema.SchemaRegistryImpl;
import com.evolveum.midpoint.prism.impl.schema.axiom.AxiomEnabledSchemaRegistry;
import com.evolveum.midpoint.prism.impl.xml.GlobalDynamicNamespacePrefixMapper;
import com.evolveum.midpoint.schema.xjc.schema.PrismCodeGenerator;
import com.evolveum.midpoint.util.exception.SchemaException;

public class CodeGeneratorTest {
    private String SCHEMA_PROJECT="../schema/";
    private static final QName VALUE_METADATA_TYPE = new QName("http://midpoint.evolveum.com/xml/ns/public/common/common-3", "ValueMetadataType");

    @NotNull
    private AxiomEnabledSchemaRegistry createSchemaRegistry() throws SchemaException, IOException {

        AxiomEnabledSchemaRegistry schemaRegistry = new AxiomEnabledSchemaRegistry();
        schemaRegistry.setNamespacePrefixMapper(new GlobalDynamicNamespacePrefixMapper());
        return schemaRegistry;
    }

    private File[] schemaFiles = new File[]{
        schema("../prism-impl/src/main/resources/xml/ns/public/annotation-3.xsd"),
        schema("../prism-impl/src/main/resources/xml/ns/public/types-3.xsd"),
        schema("../prism-impl/src/main/resources/xml/ns/public/query-3.xsd"),

        schema("src/main/resources/xml/ns/public/common/common-3.xsd"),
        schema("src/main/resources/xml/ns/public/common/api-types-3.xsd"),
        schema("src/main/resources/xml/ns/public/connector/icf-1/resource-schema-3.xsd"),
        schema("src/main/resources/xml/ns/public/model/scripting/scripting-3.xsd")
    };

    private PrismContextImpl createPrismContext() throws Exception {
        AxiomEnabledSchemaRegistry schemaRegistry = createSchemaRegistry();
        schemaRegistry.setValueMetadataTypeName(VALUE_METADATA_TYPE);
        for (File schemaFile: schemaFiles) {

            if (!schemaFile.exists()) {
                throw new IllegalStateException("Schema file "+schemaFile+" does not exist");
            }
            schemaRegistry.registerPrismSchemaFile(schemaFile);
        }

        schemaRegistry.setCatalogFiles(new File[] {
                //schema("../prism-impl/src/main/resources/catalog-test.xml"),
                schema("src/main/resources/META-INF/schemas-in-this-module.xml")});
        schemaRegistry.addAxiomSource(axiom(schema("src/main/resources/xml/ns/public/common/common-metadata-3.axiom")));
        PrismContextImpl context = PrismContextImpl.create(schemaRegistry);
        context.setDefinitionFactory(new SchemaDefinitionFactory());
        context.initialize();

        return context;
    }


    private AxiomModelStatementSource axiom(File schema) throws AxiomSyntaxException, IOException {
        return AxiomModelStatementSource.from(schema);
    }


    private File schema(String path) {
        return new File(SCHEMA_PROJECT, path);
    }


    @Test
    public void test() throws Exception {
       File target = new File("test");
       target.mkdirs();
       PrismContext prism = createPrismContext();
       PrismCodeGenerator codeGen = new PrismCodeGenerator(prism, target);
       codeGen.generate();
       codeGen.write();
    }
}
