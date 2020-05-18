package com.evolveum.axiom.lang.test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import com.evolveum.axiom.concepts.Lazy;
import com.evolveum.axiom.lang.api.AxiomBuiltIn;
import com.evolveum.axiom.lang.api.AxiomItemDefinition;
import com.evolveum.axiom.lang.api.AxiomSchemaContext;
import com.evolveum.axiom.lang.impl.AxiomStatementSource;
import com.evolveum.axiom.lang.impl.AxiomSyntaxException;
import com.evolveum.axiom.lang.impl.ModelReactorContext;
import com.evolveum.midpoint.tools.testng.AbstractUnitTest;

public abstract class AbstractReactorTest extends AbstractUnitTest {

    private static final String COMMON_DIR_PATH = "src/test/resources/";

    protected static AxiomSchemaContext parseFile(String name) throws AxiomSyntaxException, FileNotFoundException, IOException {
        return parseInputStream(name, new FileInputStream(COMMON_DIR_PATH + name));
    }

    protected static AxiomSchemaContext parseInputStream(String name, InputStream stream) throws AxiomSyntaxException, FileNotFoundException, IOException {
        return parseInputStream(name, stream, AxiomBuiltIn.Item.MODEL_DEFINITION);
    }

    protected static AxiomSchemaContext parseInputStream(String name, InputStream stream, AxiomItemDefinition rootItemDefinition) throws AxiomSyntaxException, FileNotFoundException, IOException {
        ModelReactorContext reactorContext =ModelReactorContext.defaultReactor();
        AxiomStatementSource statementSource = AxiomStatementSource.from(name, stream);
        reactorContext.loadModelFromSource(statementSource);
        return reactorContext.computeSchemaContext();
    }

    protected static AxiomStatementSource source(String name) throws AxiomSyntaxException, IOException {
        InputStream stream = new FileInputStream(COMMON_DIR_PATH + name);
        return AxiomStatementSource.from(name, stream);
    }
}
