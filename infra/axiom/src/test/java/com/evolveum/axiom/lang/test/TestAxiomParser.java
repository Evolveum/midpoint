/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.lang.test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.testng.annotations.Test;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.lang.api.AxiomBuiltIn;
import com.evolveum.axiom.lang.api.AxiomBuiltIn.Item;
import com.evolveum.axiom.lang.api.stmt.AxiomStatement;
import com.evolveum.axiom.lang.impl.AnyAxiomStatement;
import com.evolveum.axiom.lang.impl.AxiomIdentifierResolver;
import com.evolveum.axiom.lang.impl.AxiomStatementSource;
import com.evolveum.axiom.lang.impl.AxiomStatementStreamBuilder;
import com.evolveum.axiom.lang.impl.AxiomSyntaxException;
import com.evolveum.midpoint.tools.testng.AbstractUnitTest;

public class TestAxiomParser extends AbstractUnitTest {

    private static String COMMON_DIR_PATH = "src/test/resources/";
    private static String NAME = "base-example.axiom";


    @Test
    public void moduleHeaderTest() throws IOException, AxiomSyntaxException {
        AxiomStatement<?> root = parseFile(NAME);
        assertNotNull(root);
        assertEquals(root.keyword(), Item.MODEL_DEFINITION.identifier());
        assertNotNull(root.first(Item.DOCUMENTATION).get().value());
        assertEquals(root.first(Item.TYPE_DEFINITION).get().first(Item.IDENTIFIER).get().value(), AxiomIdentifier.axiom("Example"));

    }

    private AxiomStatement<?> parseFile(String name) throws AxiomSyntaxException, FileNotFoundException, IOException {
        AxiomStatementSource statementSource = AxiomStatementSource.from(new FileInputStream(COMMON_DIR_PATH + name));
        assertNotNull(statementSource);
        //assertEquals(statementSource.getModelName(), NAME);

        AxiomStatementStreamBuilder builder = AxiomStatementStreamBuilder.create(AxiomBuiltIn.Item.MODEL_DEFINITION,AnyAxiomStatement.FACTORY);

        statementSource.stream(AxiomIdentifierResolver.AXIOM_DEFAULT_NAMESPACE, builder);
        AxiomStatement<?> root = builder.result();
        return root;
    }
}
