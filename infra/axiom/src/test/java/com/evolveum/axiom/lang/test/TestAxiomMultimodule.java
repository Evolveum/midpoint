/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.lang.test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import org.testng.annotations.Test;


import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.concepts.Lazy;
import com.evolveum.axiom.lang.api.AxiomBuiltIn;
import com.evolveum.axiom.lang.api.AxiomItemDefinition;
import com.evolveum.axiom.lang.api.AxiomSchemaContext;
import com.evolveum.axiom.lang.api.AxiomTypeDefinition;
import com.evolveum.axiom.lang.api.AxiomBuiltIn.Item;
import com.evolveum.axiom.lang.api.AxiomBuiltIn.Type;

import com.evolveum.axiom.lang.impl.AxiomStatementSource;
import com.evolveum.axiom.lang.impl.ModelReactorContext;
import com.evolveum.axiom.lang.spi.AxiomSyntaxException;
import com.evolveum.midpoint.tools.testng.AbstractUnitTest;

public class TestAxiomMultimodule extends AbstractReactorTest {

    private static final String DIR = "multimodule/";
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

        //folowupContext.loadModelFromSource(statementSource);
        AxiomSchemaContext selfparsedContext = context.computeSchemaContext();
        assertNotNull(selfparsedContext.getRoot(Item.MODEL_DEFINITION.name()));
        assertTrue(selfparsedContext.getType(Type.IDENTIFIER_DEFINITION.name()).get().item(Item.ID_MEMBER.name()).get().required());
    }




}
