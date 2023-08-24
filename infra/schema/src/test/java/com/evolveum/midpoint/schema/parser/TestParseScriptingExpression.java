/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.parser;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.*;

import org.testng.annotations.Test;

import jakarta.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.io.File;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

public class TestParseScriptingExpression extends AbstractPropertyValueParserTest<ExpressionPipelineType> {

    @Override
    protected File getFile() {
        return getFile("scripting-expression");
    }

    @Test
    public void testParseToXNode() throws Exception {
        ItemDefinition<?> sequenceDef = getPrismContext().getSchemaRegistry().findItemDefinitionByElementName(SchemaConstants.S_SEQUENCE);
        System.out.println("sequence.substitutionHead = " + sequenceDef.getSubstitutionHead());
        System.out.println("sequence.heterogeneousListItem = " + sequenceDef.isHeterogeneousListItem());
        ComplexTypeDefinition sequenceCtd = getPrismContext().getSchemaRegistry().findComplexTypeDefinitionByType(sequenceDef.getTypeName());
        System.out.println("ExpressionSequenceType.list = " + sequenceCtd.isListMarker());

        System.out.println("\n\n-----------------------------------\n");

        String file = MiscUtil.readFile(getFile());
        System.out.println("Original text:\n" + file);

        RootXNode xnode = getPrismContext().parserFor(file).parseToXNode();
        System.out.println("XNode:\n" + xnode.debugDump());

        System.out.println("source -> XNode -> JSON:\n" + getPrismContext().jsonSerializer().serialize(xnode));
        System.out.println("source -> XNode -> YAML:\n" + getPrismContext().yamlSerializer().serialize(xnode));
        System.out.println("source -> XNode -> XML:\n" + getPrismContext().xmlSerializer().serialize(xnode));
    }

    @Test
    public void testParseFile() throws Exception {
        processParsings(null, null);
    }

    @Test
    public void testParseRoundTrip() throws Exception{
        processParsings(v -> getPrismContext().serializerFor(language).serialize(v), "s0");
        processParsings(v -> getPrismContext().serializerFor(language).root(new QName("dummy")).serialize(v), "s1");
        processParsings(v -> getPrismContext().serializerFor(language).root(SchemaConstantsGenerated.C_USER).serialize(v), "s2");        // misleading item name
        processParsings(v -> getPrismContext().serializerFor(language).serializeRealValue(v.getValue()), "s3");
        processParsings(v -> getPrismContext().serializerFor(language).root(new QName("dummy")).serializeAnyData(v.getValue()), "s4");
    }

    private void processParsings(SerializingFunction<PrismPropertyValue<ExpressionPipelineType>> serializer, String serId) throws Exception {
        PrismPropertyDefinition<ExpressionPipelineType> definition = getPrismContext().getSchemaRegistry().findPropertyDefinitionByElementName(SchemaConstants.S_PIPELINE);
        processParsings(ExpressionPipelineType.class, ExpressionPipelineType.COMPLEX_TYPE, definition, serializer, serId);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void assertPrismPropertyValueLocal(PrismPropertyValue<ExpressionPipelineType> value) {
        ExpressionPipelineType pipe = value.getValue();
        JAXBElement<ExpressionSequenceType> sequenceJaxb1 = (JAXBElement<ExpressionSequenceType>) pipe.getScriptingExpression().get(0);
        assertEquals("Wrong element name (1)", SchemaConstants.S_SEQUENCE, sequenceJaxb1.getName());
        assertEquals("Wrong element type (1)", ExpressionSequenceType.class, sequenceJaxb1.getValue().getClass());
        var searchJaxb1_1 = (JAXBElement<SearchExpressionType>) sequenceJaxb1.getValue().getScriptingExpression().get(0);
        assertEquals("Wrong first element name", SchemaConstants.S_SEARCH, searchJaxb1_1.getName());
        assertEquals("Wrong element type (1.1)", SearchExpressionType.class, searchJaxb1_1.getValue().getClass());
        assertEquals(new QName("RoleType"), searchJaxb1_1.getValue().getType());
        assertNotNull(searchJaxb1_1.getValue().getSearchFilter());
        var actionJaxb1_2 = (JAXBElement<ActionExpressionType>) sequenceJaxb1.getValue().getScriptingExpression().get(1);
        assertEquals("Wrong element type (1.2)", ActionExpressionType.class, actionJaxb1_2.getValue().getClass());
        assertEquals("log", actionJaxb1_2.getValue().getType());

        JAXBElement<ExpressionSequenceType> sequenceJaxb2 = (JAXBElement<ExpressionSequenceType>) pipe.getScriptingExpression().get(1);
        assertEquals("Wrong second element name", SchemaConstants.S_SEQUENCE, sequenceJaxb2.getName());
        assertEquals("Wrong element type (2)", ExpressionSequenceType.class, sequenceJaxb2.getValue().getClass());
        var actionJaxb2_1 = (JAXBElement<ActionExpressionType>) sequenceJaxb2.getValue().getScriptingExpression().get(0);
        var actionJaxb2_2 = (JAXBElement<ActionExpressionType>) sequenceJaxb2.getValue().getScriptingExpression().get(1);
        var searchJaxb2_3 = (JAXBElement<SearchExpressionType>) sequenceJaxb2.getValue().getScriptingExpression().get(2);
        assertEquals("Wrong element name (2.1)", SchemaConstants.S_ACTION, actionJaxb2_1.getName());
        assertEquals("Wrong element type (2.1)", ActionExpressionType.class, actionJaxb2_1.getValue().getClass());
        assertEquals("Wrong element name (2.2)", SchemaConstants.S_ACTION, actionJaxb2_2.getName());
        assertEquals("Wrong element type (2.2)", ActionExpressionType.class, actionJaxb2_2.getValue().getClass());
        assertEquals("Wrong element name (2.3)", SchemaConstants.S_SEARCH, searchJaxb2_3.getName());
        assertEquals("Wrong element type (2.3)", SearchExpressionType.class, searchJaxb2_3.getValue().getClass());
    }

    @Override
    protected boolean isContainer() {
        return false;
    }
}
