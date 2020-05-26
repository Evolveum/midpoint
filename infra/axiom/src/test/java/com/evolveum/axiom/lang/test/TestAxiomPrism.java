/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.lang.test;

import static org.testng.Assert.assertNotNull;

import java.io.IOException;
import org.testng.annotations.Test;


import com.evolveum.axiom.lang.api.AxiomSchemaContext;
import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.lang.impl.ModelReactorContext;
import com.evolveum.axiom.lang.spi.AxiomSyntaxException;

public class TestAxiomPrism extends AbstractReactorTest {

    private static final AxiomIdentifier PERSON = AxiomIdentifier.from("https://example.org", "Person");
    private static final AxiomIdentifier STORAGE = AxiomIdentifier.from("https://example.org/extension", "type");

    private static final AxiomIdentifier PERSON_EXTENSION = AxiomIdentifier.from("https://schema.org", "SchemaOrgPerson");
    private static final String DIR = "prism/";
    private static final String PRISM = DIR + "prism.axiom";
    private static final String COMMON_CORE = DIR + "common-core.axiom";
    private static final String COMMON_CORE_PRISM = DIR + "common-core.prism";

    private ModelReactorContext prismReactor() throws AxiomSyntaxException, IOException {
        ModelReactorContext context = ModelReactorContext.defaultReactor();
        context.loadModelFromSource(source(PRISM));
        AxiomSchemaContext schemaContext = context.computeSchemaContext();

        ModelReactorContext extendedLanguage = ModelReactorContext.reactor(schemaContext);
        extendedLanguage.loadModelFromSource(source(PRISM));
        return extendedLanguage;
    }

    @Test
    public void axiomTestPrismInAxiom() throws IOException, AxiomSyntaxException {

        ModelReactorContext extendedLanguage = prismReactor();
        extendedLanguage.loadModelFromSource(source(COMMON_CORE));
        AxiomSchemaContext schemaContext = extendedLanguage.computeSchemaContext();

        assertNotNull(schemaContext);
    }

    @Test
    public void axiomTestPrismInPrism() throws IOException, AxiomSyntaxException {

        ModelReactorContext extendedLanguage = prismReactor();
        extendedLanguage.loadModelFromSource(source(COMMON_CORE_PRISM));
        AxiomSchemaContext schemaContext = extendedLanguage.computeSchemaContext();

        assertNotNull(schemaContext);
    }

}
