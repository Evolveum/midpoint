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
import com.evolveum.axiom.lang.api.AxiomBuiltIn;
import com.evolveum.axiom.lang.api.AxiomItemDefinition;
import com.evolveum.axiom.lang.api.AxiomSchemaContext;
import com.evolveum.axiom.lang.api.AxiomTypeDefinition;
import com.evolveum.axiom.lang.api.AxiomBuiltIn.Item;
import com.evolveum.axiom.lang.api.AxiomBuiltIn.Type;

import com.evolveum.axiom.lang.impl.AxiomStatementSource;

import com.evolveum.axiom.lang.impl.AxiomSyntaxException;
import com.evolveum.axiom.lang.impl.ModelReactorContext;
import com.evolveum.midpoint.tools.testng.AbstractUnitTest;

public class TestAxiomParser extends AbstractUnitTest {

    private static final String COMMON_DIR_PATH = "src/test/resources/";
    private static final String AXIOM_LANG = "/axiom-lang.axiom";

    private static final String BASE_EXAMPLE = "base-example.axiom";
    private static final String COMMON_CORE = "common-core.axiom";
    private static final String SCRIPTING = "scripting.axiom";

    @Test
    public void axiomSelfDescribingTest() throws IOException, AxiomSyntaxException {

        ModelReactorContext bootstrapContext = ModelReactorContext.boostrapReactor();
        InputStream stream = AxiomBuiltIn.class.getResourceAsStream(AXIOM_LANG);
        AxiomStatementSource statementSource = AxiomStatementSource.from(AXIOM_LANG, stream);
        bootstrapContext.loadModelFromSource(statementSource);
        AxiomSchemaContext modelContext = bootstrapContext.computeSchemaContext();
        assertTypedefBasetype(modelContext.getType(Type.TYPE_DEFINITION.name()));

        AxiomItemDefinition modelDef = modelContext.getRoot(Item.MODEL_DEFINITION.name()).get();
        assertEquals(modelDef.name(), Item.MODEL_DEFINITION.name());

        ModelReactorContext folowupContext = ModelReactorContext.reactor(modelContext);
        folowupContext.loadModelFromSource(statementSource);
        AxiomSchemaContext selfparsedContext = bootstrapContext.computeSchemaContext();
        assertNotNull(selfparsedContext.getRoot(Item.MODEL_DEFINITION.name()));

    }


    private void assertTypedefBasetype(Optional<AxiomTypeDefinition> optional) {
        AxiomTypeDefinition typeDef = optional.get();
        assertNotNull(typeDef);
        assertEquals(typeDef.superType().get().name(), Type.BASE_DEFINITION.name());
    }


    private void assertInstanceOf(Class<?> clz, Object value) {
        assertTrue(clz.isInstance(value));
    }

    @Test
    public void moduleHeaderTest() throws IOException, AxiomSyntaxException {
        AxiomSchemaContext context = parseFile(BASE_EXAMPLE);
        assertNotNull(context.getType(AxiomIdentifier.axiom("Example")).get());
    }

    @Test
    public void commonCoreTest() throws IOException, AxiomSyntaxException {
        AxiomSchemaContext context = parseFile(COMMON_CORE);
    }

    @Test
    public void scriptingTest() throws IOException, AxiomSyntaxException {
        AxiomSchemaContext context = parseFile(SCRIPTING);
    }

    private AxiomSchemaContext parseFile(String name) throws AxiomSyntaxException, FileNotFoundException, IOException {
        return parseInputStream(name, new FileInputStream(COMMON_DIR_PATH + name));
    }

    private AxiomSchemaContext parseInputStream(String name, InputStream stream) throws AxiomSyntaxException, FileNotFoundException, IOException {
        return parseInputStream(name, stream, AxiomBuiltIn.Item.MODEL_DEFINITION);
    }

    private AxiomSchemaContext parseInputStream(String name, InputStream stream, AxiomItemDefinition rootItemDefinition) throws AxiomSyntaxException, FileNotFoundException, IOException {
        ModelReactorContext reactorContext =ModelReactorContext.defaultReactor();
        AxiomStatementSource statementSource = AxiomStatementSource.from(name, stream);
        reactorContext.loadModelFromSource(statementSource);
        return reactorContext.computeSchemaContext();
    }


}
