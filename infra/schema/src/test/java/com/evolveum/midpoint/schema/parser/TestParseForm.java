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
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.testng.annotations.Test;
import org.w3c.dom.Element;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.io.File;
import java.util.List;

import static com.evolveum.midpoint.schema.TestConstants.*;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

/**
 * @author semancik
 *
 */
public class TestParseForm extends AbstractObjectParserTest<FormType> {

	@Override
	protected File getFile() {
		return getFile("form");
	}

	@Test
	public void testParseToXNode() throws Exception {
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		RootXNode node = prismContext.parserFor(getFile()).parseToXNode();
		System.out.println("Parsed to XNode:");
		System.out.println(node.debugDump());
		System.out.println("XML -> XNode -> XML:\n" + prismContext.xmlSerializer().serialize(node));
		System.out.println("XML -> XNode -> JSON:\n" + prismContext.jsonSerializer().serialize(node));
		System.out.println("XML -> XNode -> YAML:\n" + prismContext.yamlSerializer().serialize(node));
	}

	@Test
	public void testParseFileAsPCV() throws Exception {
		displayTestTitle("testParseFileAsPCV");
		processParsingsPCV(null, null);
	}

	@Test
	public void testParseFileAsPO() throws Exception {
		displayTestTitle("testParseFileAsPO");
		processParsingsPO(null, null, true);
	}

	@Test
	@SuppressWarnings("Duplicates")
	public void testParseRoundTripAsPCV() throws Exception{
		displayTestTitle("testParseRoundTripAsPCV");

		processParsingsPCV(v -> getPrismContext().serializerFor(language).serialize(v), "s0");
		processParsingsPCV(v -> getPrismContext().serializerFor(language).root(new QName("dummy")).serialize(v), "s1");
		processParsingsPCV(v -> getPrismContext().serializerFor(language).root(SchemaConstantsGenerated.C_SYSTEM_CONFIGURATION).serialize(v), "s2");		// misleading item name
		processParsingsPCV(v -> getPrismContext().serializerFor(language).serializeRealValue(v.asContainerable()), "s3");
		processParsingsPCV(v -> getPrismContext().serializerFor(language).root(new QName("dummy")).serializeAnyData(v.asContainerable()), "s4");
	}

	@Test
	@SuppressWarnings("Duplicates")
	public void testParseRoundTripAsPO() throws Exception{
		displayTestTitle("testParseRoundTripAsPO");

		processParsingsPO(v -> getPrismContext().serializerFor(language).serialize(v), "s0", true);
		processParsingsPO(v -> getPrismContext().serializerFor(language).root(new QName("dummy")).serialize(v), "s1", false);
		processParsingsPO(v -> getPrismContext().serializerFor(language).root(SchemaConstantsGenerated.C_SYSTEM_CONFIGURATION).serialize(v), "s2", false);		// misleading item name
		processParsingsPO(v -> getPrismContext().serializerFor(language).serializeRealValue(v.asObjectable()), "s3", false);
		processParsingsPO(v -> getPrismContext().serializerFor(language).root(new QName("dummy")).serializeAnyData(v.asObjectable()), "s4", false);
	}

	private void processParsingsPCV(SerializingFunction<PrismContainerValue<FormType>> serializer, String serId) throws Exception {
		processParsings(FormType.class, null, FormType.COMPLEX_TYPE, null, serializer, serId);
	}

	private void processParsingsPO(SerializingFunction<PrismObject<FormType>> serializer, String serId, boolean checkItemName) throws Exception {
		processObjectParsings(FormType.class, FormType.COMPLEX_TYPE, serializer, serId, checkItemName);
	}

	@Override
	protected void assertPrismContainerValueLocal(PrismContainerValue<FormType> value) throws SchemaException {
		PrismObject form = value.asContainerable().asPrismObject();
		form.checkConsistence();
		assertFormPrism(form, false);
		assertFormJaxb(value.asContainerable(), false);
	}

	@Override
	protected void assertPrismObjectLocal(PrismObject<FormType> form) throws SchemaException {
		assertFormPrism(form, true);
		assertFormJaxb(form.asObjectable(), true);
		form.checkConsistence(true, true);
	}

	private void assertFormPrism(PrismObject<FormType> form, boolean isObject) {
		if (isObject) {
			assertEquals("Wrong oid", "2f9b9299-6f45-498f-bc8e-8d17c6b93b20", form.getOid());
		}
		PrismObjectDefinition<FormType> usedDefinition = form.getDefinition();
		assertNotNull("No form definition", usedDefinition);
		PrismAsserts.assertObjectDefinition(usedDefinition, new QName(SchemaConstantsGenerated.NS_COMMON, "form"),
				FormType.COMPLEX_TYPE, FormType.class);
		assertEquals("Wrong class in form", FormType.class, form.getCompileTimeClass());
		FormType formType = form.asObjectable();
		assertNotNull("asObjectable resulted in null", formType);

		assertPropertyValue(form, "name", PrismTestUtil.createPolyString("form1"));
		assertPropertyDefinition(form, "name", PolyStringType.COMPLEX_TYPE, 0, 1);

		PrismProperty<FormDefinitionType> formDefinition = form.findProperty(FormType.F_FORM_DEFINITION);
		assertNotNull("no formDefinition property", formDefinition);

	}

	private void assertFormJaxb(FormType form, boolean isObject) throws SchemaException {
		assertEquals("Wrong name", PrismTestUtil.createPolyStringType("form1"), form.getName());
		FormDefinitionType formDefinition = form.getFormDefinition();
		assertNotNull("no formDefinition value", formDefinition);
		assertEquals("Wrong formDefinition/display/label", "some label", formDefinition.getDisplay().getLabel());
		assertEquals("Wrong formDefinition/display/tooltip", "some tooltip", formDefinition.getDisplay().getTooltip());
		FormItemsType formItems = formDefinition.getFormItems();
		assertNotNull("no formItems", formItems);
		assertEquals("wrong # of form items", 3, formItems.getFormItem().size());
		assertFormItem(formItems, "main list", 0, SchemaConstantsGenerated.C_FORM_FIELD, FormFieldType.class, "FamilyName");
		assertFormItem(formItems, "main list", 1, SchemaConstantsGenerated.C_FORM_FIELD_GROUP, FormFieldGroupType.class, "Address");
		assertFormItem(formItems, "main list", 2, SchemaConstantsGenerated.C_FORM_FIELD, FormFieldType.class, "Email");
		FormItemsType itemsInGroup1 = ((FormFieldGroupType) formItems.getFormItem().get(1).getValue()).getFormItems();
		assertFormItem(itemsInGroup1, "group", 0, SchemaConstantsGenerated.C_FORM_FIELD, FormFieldType.class, "City");
		assertFormItem(itemsInGroup1, "group", 1, SchemaConstantsGenerated.C_FORM_FIELD, FormFieldType.class, "Country");
	}

	private void assertFormItem(FormItemsType formItems, String context, int index, QName elementName, Class<? extends AbstractFormItemType> clazz,
			String name) {
		String ctx = "Problem in " + context + ", item #" + index + ": ";
		JAXBElement<? extends AbstractFormItemType> itemElement = formItems.getFormItem().get(index);
		assertTrue(ctx+"Unexpected item name: "+itemElement.getName()+", expected: "+elementName, elementName.equals(itemElement.getName()));
		assertEquals(ctx+"Wrong class", clazz, itemElement.getValue().getClass());
		assertEquals(ctx+"Wrong name", name, itemElement.getValue().getName());
	}

}
