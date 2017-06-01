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

import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.PipelineDataType;
import org.apache.commons.lang3.StringUtils;
import org.testng.annotations.Test;

import javax.xml.namespace.QName;
import java.io.File;

import static org.testng.AssertJUnit.assertEquals;

/**
 * @author mederly
 *
 */
public class TestParseScriptOutput extends AbstractPropertyValueParserTest<PipelineDataType> {

	@Override
	protected File getFile() {
		return getFile("script-output");
	}

	@Test
	public void testParseToXNode() throws Exception {
		displayTestTitle("testParseToXNode");

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
		displayTestTitle("testParseFile");
		processParsings(null, null);
	}

	@Test
	public void testParseRoundTrip() throws Exception {
		displayTestTitle("testParseRoundTrip");

		processParsings(v -> getPrismContext().serializerFor(language).serialize(v), "s0");
		processParsings(v -> getPrismContext().serializerFor(language).root(new QName("dummy")).serialize(v), "s1");
		processParsings(v -> getPrismContext().serializerFor(language).root(SchemaConstantsGenerated.C_USER).serialize(v), "s2");		// misleading item name
		processParsings(v -> getPrismContext().serializerFor(language).serializeRealValue(v.getValue()), "s3");
		processParsings(v -> getPrismContext().serializerFor(language).root(new QName("dummy")).serializeAnyData(v.getValue()), "s4");
	}

	@Test
	public void testNamespaces() throws Exception {
		String file = MiscUtil.readFile(getFile());
		RootXNode xnode = getPrismContext().parserFor(file).parseToXNode();
		System.out.println("XNode:\n" + xnode.debugDump());

		String serialized = getPrismContext().xmlSerializer().serialize(xnode);
		System.out.println("XML: " + serialized);

		final String common_3 = "http://midpoint.evolveum.com/xml/ns/public/common/common-3";

		int count = StringUtils.countMatches(serialized, common_3);
		assertEquals("Wrong # of occurrences of '" + common_3 + "' in serialized form", 2, count);
	}

//	private void assertNamespaceDeclarations(String context, Element element, String... prefixes) {
//		assertEquals("Wrong namespace declarations for " + context, new HashSet<>(Arrays.asList(prefixes)),
//				DOMUtil.getNamespaceDeclarations(element).keySet());
//	}

	private void processParsings(SerializingFunction<PrismPropertyValue<PipelineDataType>> serializer, String serId) throws Exception {
		PrismPropertyDefinition definition = getPrismContext().getSchemaRegistry().findPropertyDefinitionByElementName(SchemaConstants.S_PIPELINE_DATA);
		processParsings(PipelineDataType.class, PipelineDataType.COMPLEX_TYPE, definition, serializer, serId);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void assertPrismPropertyValueLocal(PrismPropertyValue<PipelineDataType> value) throws SchemaException {
		// TODO
	}

	@Override
	protected boolean isContainer() {
		return false;
	}
}
