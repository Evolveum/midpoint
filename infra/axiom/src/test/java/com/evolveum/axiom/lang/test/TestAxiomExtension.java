/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.lang.test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import org.testng.annotations.Test;

import com.evolveum.axiom.api.AxiomName;
import com.evolveum.axiom.api.AxiomStructuredValue;
import com.evolveum.axiom.api.AxiomValue;
import com.evolveum.axiom.api.AxiomItem;
import com.evolveum.axiom.api.schema.AxiomItemDefinition;
import com.evolveum.axiom.api.schema.AxiomSchemaContext;
import com.evolveum.axiom.api.schema.AxiomTypeDefinition;
import com.evolveum.axiom.api.stream.AxiomItemTarget;
import com.evolveum.axiom.lang.antlr.AntlrDecoder;
import com.evolveum.axiom.lang.antlr.AntlrDecoderContext;
import com.evolveum.axiom.lang.antlr.AxiomAntlrStatementSource;
import com.evolveum.axiom.lang.antlr.AxiomDecoderContext;
import com.evolveum.axiom.lang.api.AxiomBuiltIn.Type;
import com.evolveum.axiom.lang.impl.ModelReactorContext;
import com.evolveum.axiom.lang.spi.AxiomNameResolver;
import com.evolveum.axiom.lang.spi.AxiomSyntaxException;


public class TestAxiomExtension extends AbstractReactorTest {

    private static final AxiomName PERSON = AxiomName.from("https://example.org", "Person");
    private static final AxiomName STORAGE = AxiomName.from("https://example.org/extension", "type");

    private static final AxiomName PERSON_EXTENSION = AxiomName.from("https://schema.org", "SchemaOrgPerson");
    private static final String DIR = "multimodel/extension/";
    private static final String SCHEMA_ORG = DIR + "person-extension.axiom";
    private static final String BASE = DIR + "test-person.axiom";
    private static final String ORDER = DIR + "declaration-order.axiom";
    private static final String LANG_EXT = DIR + "language-extension.axiom";
    private static final String LANG_EXT_USE = DIR + "language-extension-use.axiom";
    private static final String METADATA_EXT = DIR + "metadata.axiom";
    private static final AxiomName METADATA_MODIFIED = AxiomName.from("https://example.org/metadata", "modified");


    private static final String DERIVED = "multimodel/derived/";
    private static final String BASE_PERSON = DERIVED + "base-person.axiom";
    private static final String DERIVED_PERSON = DERIVED + "derived-person.axiom";
    private static final String JOHN_DOE_SUBSTITUTION_FILE = DIR + "john-doe-substitution.axiomd";

    private static final AxiomName DERIVED_PERSON_TYPE = AxiomName.from("https://example.org/derived", "Person");

    private static final AxiomName NAME = AxiomName.from("https://example.org/base", "name");
    private static final AxiomName FIRST_NAME = DERIVED_PERSON_TYPE.localName("firstName");


    @Test
    public void axiomTestExtension() throws IOException, AxiomSyntaxException {
        ModelReactorContext context = ModelReactorContext.defaultReactor();
        context.loadModelFromSource(source(SCHEMA_ORG));
        context.loadModelFromSource(source(BASE));
        AxiomSchemaContext schemaContext = context.computeSchemaContext();
        Optional<AxiomTypeDefinition> personDef = schemaContext.getType(PERSON);
        assertTrue(personDef.isPresent());
        Optional<AxiomTypeDefinition> extPersonDef = schemaContext.getType(PERSON_EXTENSION);
        assertTrue(extPersonDef.isPresent());

        for(AxiomName item : extPersonDef.get().itemDefinitions().keySet()) {
            assertTrue(personDef.get().itemDefinition(item).isPresent());
        }
    }

    @Test
    public void axiomTestOrder() throws IOException, AxiomSyntaxException {
        ModelReactorContext context = ModelReactorContext.defaultReactor();
        context.loadModelFromSource(source(ORDER));
        AxiomSchemaContext schemaContext = context.computeSchemaContext();

        AxiomTypeDefinition langExtDef = schemaContext.getType(Type.AUGMENTATION_DEFINITION.name()).get();

        Optional<AxiomTypeDefinition> personDef = schemaContext.getType(PERSON);
        assertTrue(personDef.isPresent());
        assertEquals(2, personDef.get().itemDefinitions().entrySet().size());
    }

    @Test
    public void axiomTestLanguageExtension() throws IOException, AxiomSyntaxException {

        assertTrue(Type.AUGMENTATION_DEFINITION.isSubtypeOf(Type.TYPE_DEFINITION));

        ModelReactorContext context = ModelReactorContext.defaultReactor();
        context.loadModelFromSource(source(LANG_EXT));
        AxiomSchemaContext schemaContext = context.computeSchemaContext();

        AxiomTypeDefinition typeDef = schemaContext.getType(Type.TYPE_DEFINITION.name()).get();
        assertNotNull(typeDef.itemDefinition(STORAGE).get());

        ModelReactorContext extendedLanguage = ModelReactorContext.reactor(schemaContext);
        extendedLanguage.loadModelFromSource(source(LANG_EXT));
        extendedLanguage.loadModelFromSource(source(LANG_EXT_USE));
        schemaContext = extendedLanguage.computeSchemaContext();

        Optional<AxiomTypeDefinition> personDef = schemaContext.getType(PERSON);
        assertTrue(personDef.isPresent());

        AxiomItem<?> extension = personDef.get().asComplex().get().item(STORAGE).get();

        assertFalse(extension.values().isEmpty(), "Extension statements should be available.");
        assertEquals(2, personDef.get().itemDefinitions().entrySet().size());
    }

    @Test
    public void axiomTestMetadata() throws AxiomSyntaxException, IOException {
        ModelReactorContext context = ModelReactorContext.defaultReactor();
        context.loadModelFromSource(source(METADATA_EXT));
        context.loadModelFromSource(source(BASE_PERSON));
        context.loadModelFromSource(source(DERIVED_PERSON));
        AxiomSchemaContext schemaContext = context.computeSchemaContext();
        AxiomTypeDefinition metadataTypeDef = schemaContext.getType(AxiomValue.METADATA_TYPE).get();
        Map<AxiomName, AxiomItemDefinition> defs = metadataTypeDef.itemDefinitions();
        assertFalse(defs.isEmpty());
        metadataTypeDef.itemDefinition(METADATA_MODIFIED).isPresent();


        AxiomAntlrStatementSource stream = dataSource(JOHN_DOE_SUBSTITUTION_FILE);
        AxiomItemTarget target = new AxiomItemTarget(schemaContext);
        stream.stream(target, AntlrDecoderContext.BUILTIN_DECODERS, AxiomNameResolver.defaultNamespace(DERIVED_PERSON_TYPE.namespace())
                .orPrefix("mymeta", METADATA_MODIFIED.namespace()));
        AxiomItem<?> root = target.get();
        assertEquals(root.name(), DERIVED_PERSON_TYPE.localName("person"));
        AxiomStructuredValue person = root.onlyValue().asComplex().get();
        assertEquals(person.item(NAME).get().onlyValue().value(), "John Doe");
        assertEquals(person.item(FIRST_NAME).get().onlyValue().value(), "John");
        assertEquals(person.metadata(METADATA_MODIFIED).get().onlyValue().value(), "structure");
        assertEquals(person.item(FIRST_NAME).get().onlyValue().metadata(METADATA_MODIFIED).get().onlyValue().value(), "substitution");


    }

}
