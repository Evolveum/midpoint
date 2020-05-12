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
import java.util.Collection;
import java.util.List;

import org.testng.annotations.Test;


import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.lang.api.AxiomBuiltIn;
import com.evolveum.axiom.lang.api.AxiomItemDefinition;
import com.evolveum.axiom.lang.api.AxiomTypeDefinition;
import com.evolveum.axiom.lang.api.AxiomBuiltIn.Item;
import com.evolveum.axiom.lang.api.AxiomBuiltIn.Type;
import com.evolveum.axiom.lang.api.stmt.AxiomStatement;
import com.evolveum.axiom.lang.impl.AxiomItemDefinitionImpl;

import com.evolveum.axiom.lang.impl.AxiomStatementSource;

import com.evolveum.axiom.lang.impl.AxiomSyntaxException;
import com.evolveum.axiom.lang.impl.AxiomTypeDefinitionImpl;
import com.evolveum.axiom.lang.impl.BasicStatementRule;
import com.evolveum.axiom.lang.impl.ModelReactorContext;
import com.evolveum.midpoint.tools.testng.AbstractUnitTest;
import com.google.common.collect.Iterables;

public class TestAxiomParser extends AbstractUnitTest {

    private static final String COMMON_DIR_PATH = "src/test/resources/";
    private static final String NAME = "base-example.axiom";
    private static final String AXIOM_LANG = "/axiom-lang.axiom";


    @Test
    public void axiomLanguageDefTest() throws IOException, AxiomSyntaxException {
        List<AxiomStatement<?>> roots = parseInputStream(AXIOM_LANG,AxiomBuiltIn.class.getResourceAsStream(AXIOM_LANG));
        assertNotNull(roots);
    }


    @Test
    public void axiomSelfDescribingTest() throws IOException, AxiomSyntaxException {

        ModelReactorContext bootstrapContext =createReactor(Item.MODEL_DEFINITION);
        InputStream stream = AxiomBuiltIn.class.getResourceAsStream(AXIOM_LANG);
        bootstrapContext.addStatementFactory(Type.TYPE_DEFINITION.identifier(), AxiomTypeDefinitionImpl.FACTORY);
        bootstrapContext.addStatementFactory(Type.ITEM_DEFINITION.identifier(), AxiomItemDefinitionImpl.FACTORY);

        AxiomStatementSource statementSource = AxiomStatementSource.from(AXIOM_LANG, stream);
        bootstrapContext.loadModelFromSource(statementSource);
        List<AxiomStatement<?>> roots = bootstrapContext.process();
        assertNotNull(roots);
        AxiomStatement<?> model = roots.get(0);
        Collection<AxiomStatement<?>> typeDefs = model.children(Item.TYPE_DEFINITION.identifier());
        for (AxiomStatement<?> typeDef : typeDefs) {
            assertInstanceOf(AxiomTypeDefinition.class, typeDef);
        }
        AxiomItemDefinition modelDef = model.first(Item.ROOT_DEFINITION.identifier(), AxiomItemDefinition.class).get();
        assertEquals(modelDef.identifier(), Item.MODEL_DEFINITION.identifier());

        ModelReactorContext folowupContext = createReactor(modelDef);
        folowupContext.loadModelFromSource(statementSource);
        List<AxiomStatement<?>> folowupRoots = bootstrapContext.process();
        assertNotNull(roots);
    }


    private void assertInstanceOf(Class<?> clz, Object value) {
        assertTrue(clz.isInstance(value));

    }


    @Test
    public void moduleHeaderTest() throws IOException, AxiomSyntaxException {
        List<AxiomStatement<?>> roots = parseFile(NAME);
        AxiomStatement<?> root = roots.get(0);
        assertNotNull(root);
        assertEquals(root.keyword(), Item.MODEL_DEFINITION.identifier());
        assertNotNull(root.first(Item.DOCUMENTATION).get().value());
        assertEquals(root.first(Item.TYPE_DEFINITION).get().first(Item.IDENTIFIER).get().value(), AxiomIdentifier.axiom("Example"));

    }


    private List<AxiomStatement<?>> parseFile(String name) throws AxiomSyntaxException, FileNotFoundException, IOException {
        return parseInputStream(name, new FileInputStream(COMMON_DIR_PATH + name));
    }

    private List<AxiomStatement<?>> parseInputStream(String name, InputStream stream) throws AxiomSyntaxException, FileNotFoundException, IOException {
        return parseInputStream(name, stream, AxiomBuiltIn.Item.MODEL_DEFINITION);
    }

    private List<AxiomStatement<?>> parseInputStream(String name, InputStream stream, AxiomItemDefinition rootItemDefinition) throws AxiomSyntaxException, FileNotFoundException, IOException {

        ModelReactorContext reactorContext =createReactor(rootItemDefinition);
        AxiomStatementSource statementSource = AxiomStatementSource.from(name, stream);
        reactorContext.loadModelFromSource(statementSource);
        List<AxiomStatement<?>> roots = reactorContext.process();
        return roots;
    }

    private ModelReactorContext createReactor(AxiomItemDefinition rootItemDefinition) {
        ModelReactorContext reactorContext = new ModelReactorContext();
        reactorContext.addRules(BasicStatementRule.values());
        reactorContext.addRootItemDef(rootItemDefinition);
        return reactorContext;
    }
}
