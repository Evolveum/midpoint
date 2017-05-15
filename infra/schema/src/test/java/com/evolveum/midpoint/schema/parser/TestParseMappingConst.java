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

import static org.testng.AssertJUnit.assertEquals;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismParser;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismSerializer;
import com.evolveum.midpoint.prism.xnode.ListXNode;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.PrimitiveXNode;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.io.File;
import java.util.List;

/**
 * @author mederly
 *
 */
public class TestParseMappingConst extends AbstractPropertyValueParserTest<MappingType> {

	@Override
	protected File getFile() {
		return getFile("mapping-const");
	}

	@Test
	public void testParseFile() throws Exception {
		displayTestTitle("testParseFile");
		processParsings(null, null);
	}

	@Test
	public void testParseSerialize() throws Exception{
		displayTestTitle("testParseSerialize");
		
		PrismContext prismContext = getPrismContext();
		PrismParser parser = prismContext.parserFor(getFile());
		PrismPropertyValue<MappingType> mappingPval = parser.parseItemValue();
		
		System.out.println("\nmappingPval:\n"+mappingPval.debugDump(1));
		
		PrismSerializer<RootXNode> xserializer = prismContext.xnodeSerializer();
		RootXNode xnode = xserializer.root(new QName("dummy")).serialize(mappingPval);
		
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
		displayTestTitle("testParseRoundTrip");

		processParsings(v -> getPrismContext().serializerFor(language).root(new QName("dummy")).serialize(v), "s1");
		processParsings(v -> getPrismContext().serializerFor(language).root(SchemaConstantsGenerated.C_USER).serialize(v), "s2");		// misleading item name
	}
		
	private void processParsings(SerializingFunction<PrismPropertyValue<MappingType>> serializer, String serId) throws Exception {
		PrismPropertyDefinition<MappingType> definition = getPrismContext().getSchemaRegistry().findPropertyDefinitionByElementName(SchemaConstantsGenerated.C_MAPPING);
		processParsings(MappingType.class, MappingType.COMPLEX_TYPE, definition, serializer, serId);
	}

	@Override
	protected void assertPrismPropertyValueLocal(PrismPropertyValue<MappingType> value) throws SchemaException {
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
