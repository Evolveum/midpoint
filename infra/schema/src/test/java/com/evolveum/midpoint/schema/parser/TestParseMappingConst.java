/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.parser;

import static org.testng.AssertJUnit.assertEquals;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismParser;
import com.evolveum.midpoint.prism.PrismSerializer;
import com.evolveum.midpoint.prism.xnode.*;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import jakarta.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.io.File;
import java.util.List;

public class TestParseMappingConst extends AbstractContainerValueParserTest<MappingType> {

    @Override
    protected File getFile() {
        return getFile("mapping-const");
    }

    @Test
    public void testParseFile() throws Exception {
        processParsings(null, null);
    }

    @Test
    public void testParseSerialize() throws Exception{
        PrismContext prismContext = getPrismContext();
        PrismParser parser = prismContext.parserFor(getFile());
        PrismContainerValue<MappingType> mappingPval = parser.parseItemValue();

        System.out.println("\nmappingPval:\n"+mappingPval.debugDump(1));

        PrismSerializer<RootXNode> serializer = prismContext.xnodeSerializer();
        RootXNode xnode = serializer.root(new QName("dummy")).serialize(mappingPval);

        System.out.println("\nSerialized xnode:\n"+xnode.debugDump(1));
        MapXNode xexpression = (MapXNode)((MapXNode)xnode.getSubnode()).get(new QName("expression"));
        ListXNode xconstList = (ListXNode) xexpression.get(new QName("const"));
        XNode xconst = xconstList.get(0);
        if (!(xconst instanceof PrimitiveXNode<?>)) {
            AssertJUnit.fail("const is not primitive: "+xconst);
        }
    }

    @Test
    public void testParseRoundTrip() throws Exception{
        processParsings(v -> getPrismContext().serializerFor(language).root(new QName("dummy")).serialize(v), "s1");
        processParsings(v -> getPrismContext().serializerFor(language).root(SchemaConstantsGenerated.C_USER).serialize(v), "s2");        // misleading item name
    }

    private void processParsings(SerializingFunction<PrismContainerValue<MappingType>> serializer, String serId) throws Exception {
        processParsings(MappingType.class, null, MappingType.COMPLEX_TYPE, null, serializer, serId);
    }

    @Override
    protected void assertPrismContainerValueLocal(PrismContainerValue<MappingType> value) throws SchemaException {
        MappingType mappingType = value.getValue();
        ExpressionType expressionType = mappingType.getExpression();
        List<JAXBElement<?>> expressionEvaluatorElements = expressionType.getExpressionEvaluator();
        assertEquals("Wrong number of expression evaluator elemenets", 1, expressionEvaluatorElements.size());
        JAXBElement<?> expressionEvaluatorElement = expressionEvaluatorElements.get(0);
        Object evaluatorElementObject = expressionEvaluatorElement.getValue();
        if (!(evaluatorElementObject instanceof ConstExpressionEvaluatorType)) {
                AssertJUnit.fail("Const expression is of type "
                        + evaluatorElementObject.getClass().getName());
        }
        ConstExpressionEvaluatorType constExpressionEvaluatorType = (ConstExpressionEvaluatorType)evaluatorElementObject;
        System.out.println("ConstExpressionEvaluatorType: "+constExpressionEvaluatorType);
        assertEquals("Wrong value in const evaluator", "foo", constExpressionEvaluatorType.getValue());
    }

    @Override
    protected boolean isContainer() {
        return false;
    }
}
