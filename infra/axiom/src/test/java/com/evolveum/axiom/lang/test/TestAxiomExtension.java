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

import java.io.IOException;
import java.util.Collection;
import java.util.Optional;

import org.testng.annotations.Test;


import com.evolveum.axiom.lang.api.AxiomSchemaContext;
import com.evolveum.axiom.lang.api.AxiomTypeDefinition;
import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.lang.api.AxiomBuiltIn.Type;
import com.evolveum.axiom.lang.impl.ModelReactorContext;
import com.evolveum.axiom.lang.spi.AxiomStatement;
import com.evolveum.axiom.lang.spi.AxiomSyntaxException;

public class TestAxiomExtension extends AbstractReactorTest {

    private static final AxiomIdentifier PERSON = AxiomIdentifier.from("https://example.org", "Person");
    private static final AxiomIdentifier STORAGE = AxiomIdentifier.from("https://example.org/extension", "type");

    private static final AxiomIdentifier PERSON_EXTENSION = AxiomIdentifier.from("https://schema.org", "SchemaOrgPerson");
    private static final String DIR = "multimodel/extension/";
    private static final String SCHEMA_ORG = DIR + "person-extension.axiom";
    private static final String BASE = DIR + "test-person.axiom";
    private static final String ORDER = DIR + "declaration-order.axiom";
    private static final String LANG_EXT = DIR + "language-extension.axiom";
    private static final String LANG_EXT_USE = DIR + "language-extension-use.axiom";

    @Test
    public void axiomTestExtension() throws IOException, AxiomSyntaxException {
        ModelReactorContext context = ModelReactorContext.defaultReactor();
        context.loadModelFromSource(source(SCHEMA_ORG));
        context.loadModelFromSource(source(BASE));
        AxiomSchemaContext schemaContext = context.computeSchemaContext();

        AxiomTypeDefinition langExtDef = schemaContext.getType(Type.EXTENSION_DEFINITION.name()).get();
        assertTrue(!langExtDef.identifierDefinitions().isEmpty());

        Optional<AxiomTypeDefinition> personDef = schemaContext.getType(PERSON);
        assertTrue(personDef.isPresent());
        Optional<AxiomTypeDefinition> extPersonDef = schemaContext.getType(PERSON_EXTENSION);
        assertTrue(extPersonDef.isPresent());

        for(AxiomIdentifier item : extPersonDef.get().itemDefinitions().keySet()) {
            assertTrue(personDef.get().itemDefinition(item).isPresent());
        }
    }

    @Test
    public void axiomTestOrder() throws IOException, AxiomSyntaxException {
        ModelReactorContext context = ModelReactorContext.defaultReactor();
        context.loadModelFromSource(source(ORDER));
        AxiomSchemaContext schemaContext = context.computeSchemaContext();

        AxiomTypeDefinition langExtDef = schemaContext.getType(Type.EXTENSION_DEFINITION.name()).get();
        assertTrue(!langExtDef.identifierDefinitions().isEmpty());

        Optional<AxiomTypeDefinition> personDef = schemaContext.getType(PERSON);
        assertTrue(personDef.isPresent());
        assertEquals(2, personDef.get().itemDefinitions().entrySet().size());
    }

    @Test
    public void axiomTestLanguageExtension() throws IOException, AxiomSyntaxException {
        ModelReactorContext context = ModelReactorContext.defaultReactor();
        context.loadModelFromSource(source(LANG_EXT));
        AxiomSchemaContext schemaContext = context.computeSchemaContext();

        AxiomTypeDefinition typeDef = schemaContext.getType(Type.TYPE_DEFINITION.name()).get();
        assertNotNull(typeDef.itemDefinition(STORAGE).get());


        ModelReactorContext extendedLanguage = ModelReactorContext.reactor(schemaContext);
        extendedLanguage.loadModelFromSource(source(LANG_EXT));
        extendedLanguage.loadModelFromSource(source(LANG_EXT_USE));

        schemaContext = extendedLanguage.computeSchemaContext();

        AxiomTypeDefinition langExtDef = schemaContext.getType(Type.EXTENSION_DEFINITION.name()).get();
        assertTrue(!langExtDef.identifierDefinitions().isEmpty());

        Optional<AxiomTypeDefinition> personDef = schemaContext.getType(PERSON);
        assertTrue(personDef.isPresent());
        Collection<AxiomStatement<?>> extension = ((AxiomStatement<?>) personDef.get()).children(STORAGE);
        assertFalse(extension.isEmpty(), "Extension statements should be available.");

        assertEquals(2, personDef.get().itemDefinitions().entrySet().size());
    }

}
