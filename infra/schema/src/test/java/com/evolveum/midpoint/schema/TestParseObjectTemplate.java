/*
 * Copyright (c) 2010-2013 Evolveum
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
package com.evolveum.midpoint.schema;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateMappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import com.evolveum.prism.xml.ns._public.types_3.RawType;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import java.io.File;
import java.io.IOException;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.fail;

/**
 * @author semancik
 *
 */
public class TestParseObjectTemplate {

	public static final File TEST_DIR = new File("src/test/resources/object-template");
	private static final File OBJECT_TEMPLATE_FILE = new File(TEST_DIR, "object-template.xml");
	private static final File USER_TEMPLATE_FILE = new File(TEST_DIR, "user-template.xml");
	private static final File WRONG_TEMPLATE_FILE = new File(TEST_DIR, "wrong-template.xml");

	@BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}


	@Test
	public void testParseObjectTemplateFile() throws Exception {
		roundTrip("testParseObjectTemplateFile", OBJECT_TEMPLATE_FILE,
				new QName(SchemaConstantsGenerated.NS_COMMON, "objectTemplate"));
	}

	@Test
	public void testParseUserTemplateFile() throws Exception {
		roundTrip("testParseUserTemplateFile", USER_TEMPLATE_FILE,
				new QName(SchemaConstantsGenerated.NS_COMMON, "userTemplate"));
	}

	@Test
	public void testParseWrongTemplateFile() throws Exception {
		final String TEST_NAME = "testParseWrongTemplateFile";
		File file = WRONG_TEMPLATE_FILE;

		System.out.println("===[ "+TEST_NAME+" ]===");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();

		// WHEN
		try {
			PrismObject<ObjectTemplateType> object = prismContext.parseObject(file);
			System.out.println("Parsed object - SHOULD NOT OCCUR:");
			System.out.println(object.debugDump());
			fail("Object was successfully parsed while it should not!");
		}
		// THEN
		catch (SchemaException e) {
			// ok
		}
	}

	private void roundTrip(final String TEST_NAME, File file, QName elementName) throws Exception {
		System.out.println("===[ "+TEST_NAME+" ]===");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();

		// WHEN
		PrismObject<ObjectTemplateType> object = prismContext.parseObject(file);

		// THEN
		System.out.println("Parsed object:");
		System.out.println(object.debugDump());

		assertObjectTemplate(object, elementName);

		// WHEN
		String xml = prismContext.serializeObjectToString(object, PrismContext.LANG_XML);

		// THEN
		System.out.println("Serialized object:");
		System.out.println(xml);

		assertSerializedObject(xml, elementName);

		// WHEN
		PrismObject<ObjectTemplateType> reparsedObject = prismContext.parseObject(xml);

		// THEN
		System.out.println("Re-parsed object:");
		System.out.println(reparsedObject.debugDump());

		assertObjectTemplate(reparsedObject, elementName);
        assertObjectTemplateInternals(reparsedObject, elementName);
	}

	private void assertObjectTemplate(PrismObject<ObjectTemplateType> object, QName elementName) {
		object.checkConsistence();
		assertObjectTemplatePrism(object, elementName);
	}

	private void assertObjectTemplatePrism(PrismObject<ObjectTemplateType> object, QName elementName) {

		assertEquals("Wrong oid", "10000000-0000-0000-0000-000000000002", object.getOid());
		PrismObjectDefinition<ObjectTemplateType> objectDefinition = object.getDefinition();
		assertNotNull("No object definition", objectDefinition);
		PrismAsserts.assertObjectDefinition(objectDefinition, elementName,
				ObjectTemplateType.COMPLEX_TYPE, ObjectTemplateType.class);
		assertEquals("Wrong class", ObjectTemplateType.class, object.getCompileTimeClass());
		assertEquals("Wrong object item name", elementName, object.getElementName());
		ObjectTemplateType objectType = object.asObjectable();
		assertNotNull("asObjectable resulted in null", objectType);

		assertPropertyValue(object, "name", PrismTestUtil.createPolyString("Default User Template"));
		assertPropertyDefinition(object, "name", PolyStringType.COMPLEX_TYPE, 0, 1);

	}

    // checks raw values of mappings
    // should be called only on reparsed values in order to catch some raw-data-related serialization issues (MID-2196)
    private void assertObjectTemplateInternals(PrismObject<ObjectTemplateType> object, QName elementName) throws SchemaException {
        int assignmentValuesFound = 0;
        for (ObjectTemplateMappingType mappingType : object.asObjectable().getMapping()) {
            if (mappingType.getExpression() != null) {
                if (mappingType.getTarget() != null &&
                        mappingType.getTarget().getPath() != null &&
                        new ItemPath(UserType.F_ASSIGNMENT).equivalent(mappingType.getTarget().getPath().getItemPath())) {
                    ItemDefinition assignmentDef =
                            PrismTestUtil.getPrismContext().getSchemaRegistry()
                                    .findObjectDefinitionByCompileTimeClass(UserType.class)
                                    .findItemDefinition(UserType.F_ASSIGNMENT);
                    for (JAXBElement evaluator : mappingType.getExpression().getExpressionEvaluator()) {
                        if (evaluator.getValue() instanceof RawType) {
                            RawType rawType = (RawType) evaluator.getValue();
                            Item assignment = rawType.getParsedItem(assignmentDef);
                            System.out.println("assignment:\n" + assignment.debugDump());
                            assignmentValuesFound++;
                        }
                    }
                }
            }
        }
        assertEquals("wrong # of assignment values found in mapping", 2, assignmentValuesFound);
    }


    private void assertSerializedObject(String xml, QName elementName) {
		// TODO
	}



	private void assertPropertyDefinition(PrismContainer<?> container, String propName, QName xsdType, int minOccurs,
			int maxOccurs) {
		QName propQName = new QName(SchemaConstantsGenerated.NS_COMMON, propName);
		PrismAsserts.assertPropertyDefinition(container, propQName, xsdType, minOccurs, maxOccurs);
	}

	public static void assertPropertyValue(PrismContainer<?> container, String propName, Object propValue) {
		QName propQName = new QName(SchemaConstantsGenerated.NS_COMMON, propName);
		PrismAsserts.assertPropertyValue(container, propQName, propValue);
	}

	public static <T> void assertPropertyValues(PrismContainer<?> container, String propName, T... expectedValues) {
		QName propQName = new QName(SchemaConstantsGenerated.NS_COMMON, propName);
		PrismAsserts.assertPropertyValue(container, propQName, expectedValues);
	}


}
