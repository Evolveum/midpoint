package com.evolveum.axiom.lang.test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.io.FileInputStream;
import java.io.IOException;

import org.testng.annotations.Test;

import com.evolveum.axiom.lang.api.AxiomStatement;
import com.evolveum.axiom.lang.impl.AxiomIdentifierResolver;
import com.evolveum.axiom.lang.impl.AxiomStatementSource;
import com.evolveum.axiom.lang.impl.AxiomStatementStreamBuilder;
import com.evolveum.axiom.lang.impl.AxiomSyntaxException;
import com.evolveum.midpoint.tools.testng.AbstractUnitTest;

public class TestAxiomParser extends AbstractUnitTest {

    private static String COMMON_DIR_PATH = "src/test/resources/";
    private static String NAME = "model-header";


    @Test
    public void moduleHeaderTest() throws IOException, AxiomSyntaxException {
        AxiomStatementSource statementSource = AxiomStatementSource.from(new FileInputStream(COMMON_DIR_PATH + NAME +".axiom"));
        assertNotNull(statementSource);
        assertEquals(statementSource.getModelName(), NAME);

        AxiomStatementStreamBuilder builder = AxiomStatementStreamBuilder.create();

        statementSource.stream(AxiomIdentifierResolver.AXIOM_DEFAULT_NAMESPACE, builder);
        AxiomStatement<?> root = builder.result();
        assertNotNull(root);
    }
}
