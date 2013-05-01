/**
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.schema;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.PrismJaxbProcessor;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ExtensionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType.Filter;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static com.evolveum.midpoint.schema.TestConstants.*;

/**
 * @author semancik
 *
 */
public class TestParseObjectTemplate {
	
	public static final File TEST_DIR = new File("src/test/resources/object-template");
	private static final File OBJECT_TEMPLATE_FILE = new File(TEST_DIR, "object-template.xml");
	private static final File USER_TEMPLATE_FILE = new File(TEST_DIR, "user-template.xml");
	
	@BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}
	
	
	@Test
	public void testParseObjectTemplateFile() throws SchemaException {
		System.out.println("===[ testParseObjectTemplateFile ]===");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		
		// WHEN
		PrismObject<ObjectTemplateType> object = prismContext.parseObject(OBJECT_TEMPLATE_FILE);
		
		// THEN
		System.out.println("Parsed object template:");
		System.out.println(object.dump());
		
		assertObjectTemplate(object, "objectTemplate");
	}
	
	@Test
	public void testParseUserTemplateFile() throws SchemaException {
		System.out.println("===[ testParseUserTemplateFile ]===");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		
		// WHEN
		PrismObject<ObjectTemplateType> object = prismContext.parseObject(USER_TEMPLATE_FILE);
		
		// THEN
		System.out.println("Parsed object template:");
		System.out.println(object.dump());
		
		assertObjectTemplate(object, "userTemplate");
	}

	private void assertObjectTemplate(PrismObject<ObjectTemplateType> object, String elementName) {
		object.checkConsistence();
		assertObjectTemplatePrism(object, elementName);
	}

	private void assertObjectTemplatePrism(PrismObject<ObjectTemplateType> object, String elementName) {
		
		assertEquals("Wrong oid", "10000000-0000-0000-0000-000000000002", object.getOid());
		PrismObjectDefinition<ObjectTemplateType> usedDefinition = object.getDefinition();
		assertNotNull("No object definition", usedDefinition);
		PrismAsserts.assertObjectDefinition(usedDefinition, new QName(SchemaConstantsGenerated.NS_COMMON, elementName),
				ObjectTemplateType.COMPLEX_TYPE, ObjectTemplateType.class);
		assertEquals("Wrong class", ObjectTemplateType.class, object.getCompileTimeClass());
		ObjectTemplateType objectType = object.asObjectable();
		assertNotNull("asObjectable resulted in null", objectType);
		
		assertPropertyValue(object, "name", PrismTestUtil.createPolyString("Default User Template"));
		assertPropertyDefinition(object, "name", PolyStringType.COMPLEX_TYPE, 0, 1);
		
		assertPropertyDefinition(object, "mapping", MappingType.COMPLEX_TYPE, 0, -1);
		
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
