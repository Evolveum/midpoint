/*
 * Copyright (c) 2010-2019 Evolveum
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
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;
import org.testng.annotations.Test;

import javax.xml.namespace.QName;
import java.io.File;
import java.math.BigInteger;
import java.util.List;

import static org.testng.AssertJUnit.*;

/**
 *
 */
public class TestParseSystemConfiguration extends AbstractObjectParserTest<SystemConfigurationType> {

	@Override
	protected File getFile() {
		return getFile("system-configuration");
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
		processParsingsPCV(v -> getPrismContext().serializerFor(language).root(SchemaConstantsGenerated.C_USER).serialize(v), "s2");		// misleading item name
		processParsingsPCV(v -> getPrismContext().serializerFor(language).serializeRealValue(v.asContainerable()), "s3");
		processParsingsPCV(v -> getPrismContext().serializerFor(language).root(new QName("dummy")).serializeAnyData(v.asContainerable()), "s4");
	}

	@Test
	@SuppressWarnings("Duplicates")
	public void testParseRoundTripAsPO() throws Exception{
		displayTestTitle("testParseRoundTripAsPO");

		processParsingsPO(v -> getPrismContext().serializerFor(language).serialize(v), "s0", true);
		processParsingsPO(v -> getPrismContext().serializerFor(language).root(new QName("dummy")).serialize(v), "s1", false);
		processParsingsPO(v -> getPrismContext().serializerFor(language).root(SchemaConstantsGenerated.C_USER).serialize(v), "s2", false);		// misleading item name
		processParsingsPO(v -> getPrismContext().serializerFor(language).serializeRealValue(v.asObjectable()), "s3", false);
		processParsingsPO(v -> getPrismContext().serializerFor(language).root(new QName("dummy")).serializeAnyData(v.asObjectable()), "s4", false);
	}

	private void processParsingsPCV(SerializingFunction<PrismContainerValue<SystemConfigurationType>> serializer, String serId) throws Exception {
		processParsings(SystemConfigurationType.class, null, SystemConfigurationType.COMPLEX_TYPE, null, serializer, serId);
	}

	private void processParsingsPO(SerializingFunction<PrismObject<SystemConfigurationType>> serializer, String serId, boolean checkItemName) throws Exception {
		processObjectParsings(SystemConfigurationType.class, SystemConfigurationType.COMPLEX_TYPE, serializer, serId, checkItemName);
	}

	@Override
	protected void assertPrismContainerValueLocal(PrismContainerValue<SystemConfigurationType> value) throws SchemaException {
		PrismObject<SystemConfigurationType> object = value.asContainerable().asPrismObject();
		object.checkConsistence();
		assertPrism(object, false);
		assertJaxb(value.asContainerable(), false);
	}

	@Override
	protected void assertPrismObjectLocal(PrismObject<SystemConfigurationType> object) throws SchemaException {
		assertPrism(object, true);
		assertJaxb(object.asObjectable(), true);
		object.checkConsistence(true, true);
	}

	private void assertPrism(PrismObject<SystemConfigurationType> object, boolean isObject) {
		if (isObject) {
			assertEquals("Wrong oid", "0978a754-9224-4afe-993a-ea1528012822", object.getOid());
		}
		PrismObjectDefinition<SystemConfigurationType> usedDefinition = object.getDefinition();
		assertNotNull("No definition", usedDefinition);
		PrismAsserts.assertObjectDefinition(usedDefinition, new QName(SchemaConstantsGenerated.NS_COMMON, "systemConfiguration"),
				SystemConfigurationType.COMPLEX_TYPE, SystemConfigurationType.class);
		assertEquals("Wrong class in object", SystemConfigurationType.class, object.getCompileTimeClass());
		SystemConfigurationType config = object.asObjectable();
		assertNotNull("asObjectable resulted in null", config);

		assertPropertyValue(object, "name", PrismTestUtil.createPolyString("sysconfig"));
		assertPropertyDefinition(object, "name", PolyStringType.COMPLEX_TYPE, 0, 1);
	}

	private void assertJaxb(SystemConfigurationType config, boolean isObject) throws SchemaException {
		assertEquals("Wrong name", PrismTestUtil.createPolyStringType("sysconfig"), config.getName());
		List<NotificationMessageAttachmentType> attachments =
				config
						.getNotificationConfiguration()
						.getHandler().get(0)
						.getSimpleUserNotifier().get(0)
						.getAttachment();
		assertEquals("Wrong # of attachments", 4, attachments.size());
		assertAttachmentContent(attachments.get(0), "untyped");
		assertAttachmentContent(attachments.get(1), "ABC");
		assertAttachmentContent(attachments.get(2), "DEF".getBytes());
		assertAttachmentContent(attachments.get(3), new BigInteger("1234567890123456789012345678901234567890"));
	}

	// for byte[] content we assume convertibility to String
	private void assertAttachmentContent(NotificationMessageAttachmentType attachment, Object expected) throws SchemaException {
		Object real = RawType.getValue(attachment.getContent());
		System.out.println("Expected: " + expected + " (" + expected.getClass() + ")");
		System.out.println("Real: " + real + " (" + real.getClass() + ")");
		if (expected instanceof byte[]) {
			System.out.println("Expected (unwrapped): " + toString(expected));
			System.out.println("Real (unwrapped): " + toString(real));
			assertEquals("Wrong value", toString(expected), toString(real));
		} else {
			assertEquals("Wrong value", expected, real);
		}
	}

	private String toString(Object o) {
		if (o instanceof byte[]) {
			return new String((byte[]) o);
		} else {
			return o.toString();
		}
	}

}
