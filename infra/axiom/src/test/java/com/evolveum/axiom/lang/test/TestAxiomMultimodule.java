/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.lang.test;

import static org.testng.Assert.assertEquals;
import java.io.IOException;
import org.testng.annotations.Test;

import com.evolveum.axiom.api.schema.AxiomItemDefinition;
import com.evolveum.axiom.api.schema.AxiomSchemaContext;
import com.evolveum.axiom.lang.api.AxiomBuiltIn.Item;
import com.evolveum.axiom.lang.impl.ModelReactorContext;
import com.evolveum.axiom.lang.spi.AxiomSyntaxException;

public class TestAxiomMultimodule extends AbstractReactorTest {

    private static final String DIR = "multimodel/ref/";
    private static final String SCHEMA_ORG = DIR + "schema-org-person.axiom";
    private static final String FOAF = DIR + "foaf-person.axiom";
    private static final String TEST = DIR + "test-person.axiom";
    private static final String TEST_INVALID = DIR + "test-person.axiom.invalid";

    @Test
    public void axiomTestImports() throws IOException, AxiomSyntaxException {
        ModelReactorContext context = ModelReactorContext.defaultReactor();
        context.loadModelFromSource(source(SCHEMA_ORG));
        context.loadModelFromSource(source(FOAF));
        context.loadModelFromSource(source(TEST));
        AxiomSchemaContext schemaContext = context.computeSchemaContext();

        AxiomItemDefinition modelDef = schemaContext.getRoot(Item.MODEL_DEFINITION.name()).get();
        assertEquals(modelDef.name(), Item.MODEL_DEFINITION.name());

        }

}
