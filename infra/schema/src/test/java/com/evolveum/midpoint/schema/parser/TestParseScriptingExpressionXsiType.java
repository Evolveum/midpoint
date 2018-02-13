/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.schema.parser;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionExpressionType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ExpressionPipelineType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ExpressionSequenceType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.SearchExpressionType;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.io.File;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author mederly
 *
 * Tests whether xsi:type serialization in YAML works well. (Should be in prism module but it was easiest to create
 * this test case from existing one in schema.)
 */
@SuppressWarnings("Duplicates")
public class TestParseScriptingExpressionXsiType extends AbstractPropertyValueParserTest<ExpressionPipelineType> {

	@Override
	protected File getFile() {
		return getFile("scripting-expression-xsi-type");
	}

	@Test
	public void testYamlSerialization() throws Exception {
		displayTestTitle("testParseToXNode");

		String file = MiscUtil.readFile(getFile());
		System.out.println("Original text:\n" + file);

		RootXNode xnode = getPrismContext().parserFor(file).parseToXNode();
		System.out.println("XNode:\n" + xnode.debugDump());

		String yaml = getPrismContext().yamlSerializer().serialize(xnode);
		System.out.println("source -> XNode -> YAML:\n" + yaml);

		PrismValue value = getPrismContext().parserFor(yaml).parseItemValue();
		assertPrismPropertyValueLocal((PrismPropertyValue<ExpressionPipelineType>) value);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void assertPrismPropertyValueLocal(PrismPropertyValue<ExpressionPipelineType> value) throws SchemaException {
		ExpressionPipelineType pipe = value.getValue();
		JAXBElement<ExpressionSequenceType> sequenceJaxb1 = (JAXBElement<ExpressionSequenceType>) pipe.getScriptingExpression().get(0);
		assertEquals("Wrong element name (1)", SchemaConstants.S_SEQUENCE, sequenceJaxb1.getName());
		assertEquals("Wrong element type (1)", ExpressionSequenceType.class, sequenceJaxb1.getValue().getClass());
		JAXBElement<SearchExpressionType> searchJaxb1_1 = (JAXBElement<SearchExpressionType>) sequenceJaxb1.getValue().getScriptingExpression().get(0);
		assertEquals("Wrong first element name", SchemaConstants.S_SEARCH, searchJaxb1_1.getName());
		assertEquals("Wrong element type (1.1)", SearchExpressionType.class, searchJaxb1_1.getValue().getClass());
		assertEquals(new QName("RoleType"), searchJaxb1_1.getValue().getType());
		assertNotNull(searchJaxb1_1.getValue().getSearchFilter());
		JAXBElement<ActionExpressionType> actionJaxb1_2 = (JAXBElement<ActionExpressionType>) sequenceJaxb1.getValue().getScriptingExpression().get(1);
		assertEquals("Wrong element type (1.2)", ActionExpressionType.class, actionJaxb1_2.getValue().getClass());
		assertEquals("log", actionJaxb1_2.getValue().getType());

		JAXBElement<ExpressionSequenceType> sequenceJaxb2 = (JAXBElement<ExpressionSequenceType>) pipe.getScriptingExpression().get(1);
		assertEquals("Wrong second element name", SchemaConstants.S_SEQUENCE, sequenceJaxb2.getName());
		assertEquals("Wrong element type (2)", ExpressionSequenceType.class, sequenceJaxb2.getValue().getClass());
		JAXBElement<ActionExpressionType> actionJaxb2_1 = (JAXBElement<ActionExpressionType>) sequenceJaxb2.getValue().getScriptingExpression().get(0);
		JAXBElement<ActionExpressionType> actionJaxb2_2 = (JAXBElement<ActionExpressionType>) sequenceJaxb2.getValue().getScriptingExpression().get(1);
		JAXBElement<SearchExpressionType> searchJaxb2_3 = (JAXBElement<SearchExpressionType>) sequenceJaxb2.getValue().getScriptingExpression().get(2);
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
