package com.evolveum.axiom.lang.test;

import static org.testng.Assert.assertEquals;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

import com.evolveum.axiom.api.AxiomStructuredValue;
import com.evolveum.axiom.api.AxiomItem;
import com.evolveum.axiom.api.AxiomName;
import com.evolveum.axiom.api.AxiomValue;
import com.evolveum.axiom.api.schema.AxiomSchemaContext;
import com.evolveum.axiom.api.schema.AxiomTypeDefinition;
import com.evolveum.axiom.api.stream.AxiomItemTarget;
import com.evolveum.axiom.lang.antlr.AntlrDecoderContext;
import com.evolveum.axiom.lang.api.AxiomBuiltIn.Item;
import com.evolveum.axiom.lang.impl.ModelReactorContext;
import com.evolveum.axiom.lang.spi.AxiomSemanticException;
import com.evolveum.axiom.lang.spi.AxiomSyntaxException;
import com.evolveum.concepts.SourceLocation;

public class TestAxiomInfra extends AbstractReactorTest {

    private static final SourceLocation TEST = SourceLocation.from("test", -1, -1);

    private static final String PRISM = "prism/prism.axiom";
    private static final String PRISM_TEST = "prism/prism-infra.axiom";

    private static final AxiomName PRISM_MODEL = AxiomName.from("http://midpoint.evolveum.com/xml/ns/public/common/prism", "PrismModel");


    @Test
    void testDataStreamApi() {
        AxiomSchemaContext context = ModelReactorContext.defaultReactor().computeSchemaContext();
        AxiomItemTarget simpleItem = new AxiomItemTarget(context);
        simpleItem.startItem(Item.MODEL_DEFINITION.name(), TEST);
        simpleItem.startValue("test", TEST);
        simpleItem.endValue(TEST);
        simpleItem.endItem(TEST);

        AxiomItem<?> result = simpleItem.get();
        assertModel(result,"test");
    }

    @Test
    void testInfraValueApi() {
        AxiomSchemaContext context = ModelReactorContext.defaultReactor().computeSchemaContext();
        AxiomItemTarget simpleItem = new AxiomItemTarget(context);
        simpleItem.startItem(Item.MODEL_DEFINITION.name(), TEST);
        simpleItem.startValue(null, TEST);
        simpleItem.startInfra(AxiomValue.VALUE, TEST);
        simpleItem.startValue("test", TEST);
        simpleItem.endValue(TEST);
        simpleItem.endInfra(TEST);
        simpleItem.endValue(TEST);
        simpleItem.endItem(TEST);

        AxiomItem<?> result = simpleItem.get();
        assertModel(result,"test");
    }

    void testInfraTypeApi() throws AxiomSemanticException, AxiomSyntaxException, FileNotFoundException, IOException {
        AxiomSchemaContext context = ModelReactorContext.defaultReactor()
                .loadModelFromSource(source(PRISM))
                .computeSchemaContext();
        AxiomItemTarget t = new AxiomItemTarget(context);
        t.startItem(Item.MODEL_DEFINITION.name(), TEST); // model
        t.startValue("test", TEST);                      //   test {
        t.startInfra(AxiomValue.TYPE, TEST);             //     @type
        t.startValue(PRISM_MODEL, TEST);                 //       prism:model
        t.endValue(TEST);                                //       ;
        t.endInfra(TEST);                                //
        t.startItem(PRISM_MODEL.localName("container"), TEST);
        t.startValue("test", TEST);
        t.endValue(TEST);
        t.endItem(TEST);
        t.endValue(TEST);
        t.endItem(TEST);

        AxiomItem<?> result = t.get();
        AxiomTypeDefinition typeDef = t.get().onlyValue().type().get();
        assertEquals(typeDef.name(), PRISM_MODEL);
        assertModel(result,"test");
    }

    void testInfraSerialized() throws AxiomSemanticException, AxiomSyntaxException, FileNotFoundException, IOException {
        AxiomSchemaContext context = ModelReactorContext.defaultReactor()
                .loadModelFromSource(source(PRISM))
                .computeSchemaContext();
        AxiomItemTarget t = new AxiomItemTarget(context);
        source(PRISM_TEST).stream(t, AntlrDecoderContext.BUILTIN_DECODERS);

        AxiomItem<?> result = t.get();
        AxiomTypeDefinition typeDef = t.get().onlyValue().type().get();
        assertEquals(typeDef.name(), PRISM_MODEL);
        assertModel(result,AxiomName.from("http://example.org", "test"));
    }


    private void assertModel(AxiomItem<?> result, Object name) {
        AxiomStructuredValue model = result.onlyValue().asComplex().get();
        assertEquals(model.item(Item.NAME).get().onlyValue().value(), name);
    }
}
