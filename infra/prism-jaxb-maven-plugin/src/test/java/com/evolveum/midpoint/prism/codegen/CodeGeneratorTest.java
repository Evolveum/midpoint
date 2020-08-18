package com.evolveum.midpoint.prism.codegen;

import java.io.File;
import java.io.IOException;

import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.impl.PrismContextImpl;
import com.evolveum.midpoint.prism.impl.schema.SchemaDefinitionFactory;
import com.evolveum.midpoint.prism.impl.schema.SchemaRegistryImpl;
import com.evolveum.midpoint.prism.impl.xml.GlobalDynamicNamespacePrefixMapper;
import com.evolveum.midpoint.schema.xjc.schema.PrismCodeGenerator;
import com.evolveum.midpoint.util.exception.SchemaException;

public class CodeGeneratorTest {
    private String SCHEMA_PROJECT="../schema/";

    @NotNull
    private SchemaRegistryImpl createSchemaRegistry() throws SchemaException, IOException {

        SchemaRegistryImpl schemaRegistry = new SchemaRegistryImpl();
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
        SchemaRegistryImpl schemaRegistry = createSchemaRegistry();

        for (File schemaFile: schemaFiles) {

            if (!schemaFile.exists()) {
                throw new IllegalStateException("Schema file "+schemaFile+" does not exist");
            }
            schemaRegistry.registerPrismSchemaFile(schemaFile);
        }

        schemaRegistry.setCatalogFiles(new File[] {
                //schema("../prism-impl/src/main/resources/catalog-test.xml"),
                schema("src/main/resources/META-INF/schemas-in-this-module.xml")});
        PrismContextImpl context = PrismContextImpl.create(schemaRegistry);
        context.setDefinitionFactory(new SchemaDefinitionFactory());
        context.initialize();

        return context;
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
