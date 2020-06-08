package com.evolveum.axiom.lang.test;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.evolveum.axiom.api.AxiomComplexValue;
import com.evolveum.axiom.api.AxiomItem;
import com.evolveum.axiom.api.AxiomValue;
import com.evolveum.axiom.api.schema.AxiomSchemaContext;
import com.evolveum.axiom.api.stream.AxiomItemTarget;
import com.evolveum.axiom.concepts.SourceLocation;
import com.evolveum.axiom.lang.api.AxiomBuiltIn.Item;
import com.evolveum.axiom.lang.impl.ModelReactorContext;
import com.evolveum.axiom.lang.spi.AxiomIdentifierResolver;

public class TestAxiomInfra {

    private static final SourceLocation TEST = SourceLocation.from("test", -1, -1);

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
    void testInfraStreamApi() {
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

    private void assertModel(AxiomItem<?> result, String name) {
        AxiomComplexValue model = result.onlyValue().asComplex().get();
        assertEquals(model.item(Item.NAME).get().onlyValue().value(), name);
    }
}
