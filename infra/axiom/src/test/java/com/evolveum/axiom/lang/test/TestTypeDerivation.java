/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.lang.test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.Optional;

import org.testng.annotations.Test;

import com.evolveum.axiom.api.AxiomName;
import com.evolveum.axiom.api.AxiomComplexValue;
import com.evolveum.axiom.api.AxiomItem;
import com.evolveum.axiom.api.AxiomValue;
import com.evolveum.axiom.api.meta.Inheritance;
import com.evolveum.axiom.api.schema.AxiomItemDefinition;
import com.evolveum.axiom.api.schema.AxiomSchemaContext;
import com.evolveum.axiom.api.schema.AxiomTypeDefinition;
import com.evolveum.axiom.api.stream.AxiomItemTarget;
import com.evolveum.axiom.lang.antlr.AxiomAntlrStatementSource;
import com.evolveum.axiom.lang.impl.ModelReactorContext;
import com.evolveum.axiom.lang.spi.AxiomIdentifierResolver;
import com.evolveum.axiom.lang.spi.AxiomSyntaxException;

public class TestTypeDerivation extends AbstractReactorTest {

    private static final AxiomName DERIVED_PERSON = AxiomName.from("https://example.org/derived", "Person");
    private static final AxiomName FIRST_NAME = DERIVED_PERSON.localName("firstName");
    private static final AxiomName LAST_NAME = DERIVED_PERSON.localName("lastName");
    private static final AxiomName NAME = AxiomName.from("https://example.org/base", "name");
    private static final String DIR = "multimodel/derived/";
    private static final String BASE = DIR + "base-person.axiom";
    private static final String DERIVED = DIR + "derived-person.axiom";
    private static final String JOHN_DOE_FILE = DIR + "john-doe.axiomd";


    private AxiomSchemaContext loadModel() throws AxiomSyntaxException, IOException {
        ModelReactorContext context = ModelReactorContext.defaultReactor();
        context.loadModelFromSource(source(BASE));
        context.loadModelFromSource(source(DERIVED));
        return context.computeSchemaContext();
    }

    @Test
    public void axiomTestInheritance() throws IOException, AxiomSyntaxException {
        AxiomSchemaContext schemaContext = loadModel();

        Optional<AxiomTypeDefinition> personDef = schemaContext.getType(DERIVED_PERSON);
        assertTrue(personDef.isPresent());
        for (Entry<AxiomName, AxiomItemDefinition> idDef : personDef.get().itemDefinitions().entrySet()) {
            AxiomItemDefinition item = idDef.getValue();
            assertEquals(idDef.getKey(), Inheritance.adapt(DERIVED_PERSON, item), " should have different namespace");
        }
    }

    @Test
    public void axiomData() throws AxiomSyntaxException, FileNotFoundException, IOException {
        AxiomSchemaContext context = loadModel();
        AxiomAntlrStatementSource stream = dataSource(JOHN_DOE_FILE);
        AxiomItemTarget target = new AxiomItemTarget(context, AxiomIdentifierResolver.defaultNamespace(DERIVED_PERSON.namespace()));
        stream.stream(target);
        AxiomItem<?> root = target.get();
        assertEquals(root.name(), DERIVED_PERSON.localName("person"));
        AxiomComplexValue person = root.onlyValue().asComplex().get();
        assertEquals(person.item(NAME).get().onlyValue().value(), "John Doe");
        assertEquals(person.item(FIRST_NAME).get().onlyValue().value(), "John");


    }
}
