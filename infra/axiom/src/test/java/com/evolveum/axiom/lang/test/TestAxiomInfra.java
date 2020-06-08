package com.evolveum.axiom.lang.test;

import static org.testng.Assert.assertEquals;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.testng.annotations.Test;

import com.evolveum.axiom.api.AxiomComplexValue;
import com.evolveum.axiom.api.AxiomItem;
import com.evolveum.axiom.api.AxiomName;
import com.evolveum.axiom.api.AxiomValue;
import com.evolveum.axiom.api.schema.AxiomSchemaContext;
import com.evolveum.axiom.api.schema.AxiomTypeDefinition;
import com.evolveum.axiom.api.stream.AxiomItemTarget;
import com.evolveum.axiom.concepts.SourceLocation;
import com.evolveum.axiom.lang.api.AxiomBuiltIn.Item;
import com.evolveum.axiom.lang.impl.ModelReactorContext;
import com.evolveum.axiom.lang.spi.AxiomIdentifierResolver;
import com.evolveum.axiom.lang.spi.AxiomSemanticException;
import com.evolveum.axiom.lang.spi.AxiomSyntaxException;

public class TestAxiomInfra extends AbstractReactorTest {

    private static final SourceLocation TEST = SourceLocation.from("test", -1, -1);

    private static final String DIR = "prism/prism.axiom";
    private static final AxiomName PRISM_MODEL = AxiomName.from("http://midpoint.evolveum.com/xml/ns/public/common/prism", "PrismModel");

    private AxiomIdentifierResolver resolver;

    @Test
    void testDataStreamApi() {
        AxiomSchemaContext context = ModelReactorContext.defaultReactor().computeSchemaContext();
        AxiomItemTarget simpleItem = new AxiomItemTarget(context, resolver);
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
        AxiomItemTarget simpleItem = new AxiomItemTarget(context, resolver);
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

    @Test
    void testInfraTypeApi() throws AxiomSemanticException, AxiomSyntaxException, FileNotFoundException, IOException {
        AxiomSchemaContext context = ModelReactorContext.defaultReactor()
                .loadModelFromSource(source(DIR))
                .computeSchemaContext();
        AxiomItemTarget simpleItem = new AxiomItemTarget(context, resolver);
        simpleItem.startItem(Item.MODEL_DEFINITION.name(), TEST); // model
        simpleItem.startValue("test", TEST);                      //   test {
        simpleItem.startInfra(AxiomValue.TYPE, TEST);             //     @type
        simpleItem.startValue(PRISM_MODEL, TEST);                 //       prism:model
        simpleItem.endValue(TEST);                                //       ;
        simpleItem.endInfra(TEST);                                //
        simpleItem.endValue(TEST);
        simpleItem.endItem(TEST);

        AxiomItem<?> result = simpleItem.get();
        AxiomTypeDefinition typeDef = simpleItem.get().onlyValue().type().get();
        assertEquals(typeDef.name(), PRISM_MODEL);
        assertModel(result,"test");
    }


    private void assertModel(AxiomItem<?> result, String name) {
        AxiomComplexValue model = result.onlyValue().asComplex().get();
        assertEquals(model.item(Item.NAME).get().onlyValue().value(), name);
    }
}
