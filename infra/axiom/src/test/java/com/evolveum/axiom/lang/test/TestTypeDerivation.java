/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.lang.test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.util.Map.Entry;
import java.util.Optional;

import org.testng.annotations.Test;


import com.evolveum.axiom.lang.api.AxiomSchemaContext;
import com.evolveum.axiom.lang.api.AxiomTypeDefinition;
import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.api.meta.Inheritance;
import com.evolveum.axiom.lang.api.AxiomBuiltIn.Type;
import com.evolveum.axiom.lang.api.AxiomItemDefinition;
import com.evolveum.axiom.lang.impl.ModelReactorContext;
import com.evolveum.axiom.lang.spi.AxiomSyntaxException;

public class TestTypeDerivation extends AbstractReactorTest {

    private static final AxiomIdentifier DERIVED_PERSON = AxiomIdentifier.from("https://example.org/derived", "Person");
    private static final String DIR = "multimodel/derived/";
    private static final String BASE = DIR + "base-person.axiom";
    private static final String DERIVED = DIR + "derived-person.axiom";

    @Test
    public void axiomTestInheritance() throws IOException, AxiomSyntaxException {
        ModelReactorContext context = ModelReactorContext.defaultReactor();
        context.loadModelFromSource(source(BASE));
        context.loadModelFromSource(source(DERIVED));
        AxiomSchemaContext schemaContext = context.computeSchemaContext();

        AxiomTypeDefinition langExtDef = schemaContext.getType(Type.EXTENSION_DEFINITION.name()).get();
        assertTrue(!langExtDef.identifierDefinitions().isEmpty());

        Optional<AxiomTypeDefinition> personDef = schemaContext.getType(DERIVED_PERSON);
        assertTrue(personDef.isPresent());
        for (Entry<AxiomIdentifier, AxiomItemDefinition> idDef : personDef.get().itemDefinitions().entrySet()) {
            AxiomItemDefinition item = idDef.getValue();
            assertEquals(idDef.getKey(), Inheritance.adapt(DERIVED_PERSON, item), " should have different namespace");
        }
    }
}
