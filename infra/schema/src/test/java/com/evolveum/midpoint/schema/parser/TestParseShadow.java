/*
 * Copyright (c) 2010-2016 Evolveum
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
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.testng.annotations.Test;

import javax.xml.namespace.QName;

import static com.evolveum.midpoint.schema.TestConstants.SHADOW_FILE_BASENAME;
import static org.testng.AssertJUnit.*;

/**
 * @author semancik
 *
 */
public class TestParseShadow extends AbstractParserTest {

	@Test
	public void testParseShadowFile() throws Exception {
		displayTestTitle("testParseShadowFile");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		
		// WHEN
		PrismObject<ShadowType> object = prismContext.parseObject(getFile(SHADOW_FILE_BASENAME));
		// THEN
		System.out.println("Parsed object:");
		System.out.println(object.debugDump());
		
		String serialized = prismContext.serializerFor(language).serialize(object);
		System.out.println("Serialized: \n" +serialized);
		
		PrismObject<ShadowType> reparsed = prismContext.parseObject(serialized);
		
		assertObject(object);
	}

	@Test
	public void testParseShadowRoundTrip() throws Exception{
		displayTestTitle("testParseShadowRoundTrip");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		
		// WHEN
		PrismObject<ShadowType> object = prismContext.parseObject(getFile(SHADOW_FILE_BASENAME));
		
		// THEN
		System.out.println("Parsed object:");
		System.out.println(object.debugDump());
		
		assertObject(object);
		
		String serialized = prismContext.serializerFor(language).serialize(object);
		System.out.println("Serialized:");
		System.out.println(serialized);
		
		// REPARSE
		PrismObject<ShadowType> reparsed = prismContext.parseObject(serialized);
		
		// THEN
		System.out.println("Reparsed:");
		System.out.println(reparsed.debugDump());
		
		assertObject(reparsed);
		
		// and some sanity checks
		
		assertTrue("Object not equals", object.equals(reparsed));
		
		ObjectDelta<ShadowType> delta = object.diff(reparsed);
		assertTrue("Delta not empty", delta.isEmpty());
		
	}

	
	void assertObject(PrismObject<ShadowType> object) throws SchemaException {
		object.checkConsistence();
		assertPrism(object);
		assertJaxb(object.asObjectable());
		
		object.checkConsistence(true, false);
	}

	void assertPrism(PrismObject<ShadowType> shadow) {
		
		assertEquals("Wrong oid", "88519fca-3f4a-44ca-91c8-dc9be5bf3d03", shadow.getOid());
		PrismObjectDefinition<ShadowType> usedDefinition = shadow.getDefinition();
		assertNotNull("No object definition", usedDefinition);
		PrismAsserts.assertObjectDefinition(usedDefinition, new QName(SchemaConstantsGenerated.NS_COMMON, "shadow"),
				ShadowType.COMPLEX_TYPE, ShadowType.class);
		assertEquals("Wrong class in object", ShadowType.class, shadow.getCompileTimeClass());
		ShadowType objectType = shadow.asObjectable();
		assertNotNull("asObjectable resulted in null", objectType);
		
		assertPropertyValue(shadow, "name", PrismTestUtil.createPolyString("hbarbossa"));
		assertPropertyDefinition(shadow, "name", PolyStringType.COMPLEX_TYPE, 0, 1);

//		assertPropertyDefinition(shadow, "organizationalUnit", PolyStringType.COMPLEX_TYPE, 0, -1);
//		assertPropertyValues(shadow, "organizationalUnit",
//				new PolyString("Brethren of the Coast", "brethren of the coast"),
//				new PolyString("Davie Jones' Locker", "davie jones locker"));
		

		ItemPath admStatusPath = new ItemPath(UserType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS);
		PrismProperty<ActivationStatusType> admStatusProperty1 = shadow.findProperty(admStatusPath);
		PrismAsserts.assertDefinition(admStatusProperty1.getDefinition(), ActivationType.F_ADMINISTRATIVE_STATUS, SchemaConstants.C_ACTIVATION_STATUS_TYPE, 0, 1);
		assertNotNull("Property "+admStatusPath+" not found", admStatusProperty1);
		PrismAsserts.assertPropertyValue(admStatusProperty1, ActivationStatusType.ENABLED);
		
		PrismReference resourceRef = shadow.findReference(ShadowType.F_RESOURCE_REF);
		assertEquals("Wrong number of resourceRef values", 1, resourceRef.getValues().size());
		PrismAsserts.assertReferenceValue(resourceRef, "10000000-0000-0000-0000-000000000003");
	}
	
	private void assertJaxb(ShadowType shadow) throws SchemaException {
		assertEquals("88519fca-3f4a-44ca-91c8-dc9be5bf3d03", shadow.getOid());
		assertEquals("Wrong name", PrismTestUtil.createPolyStringType("hbarbossa"), shadow.getName());

		ActivationType activation = shadow.getActivation();
		assertNotNull("No activation", activation);
		assertEquals("User not enabled", ActivationStatusType.ENABLED, activation.getAdministrativeStatus());
		
		ObjectReferenceType resourceRef = shadow.getResourceRef();
		assertNotNull("No resourceRef", resourceRef);

		assertEquals("Wrong resourceRef oid (jaxb)", "10000000-0000-0000-0000-000000000003", resourceRef.getOid());
		assertEquals("Wrong resourceRef type (jaxb)", ResourceType.COMPLEX_TYPE, resourceRef.getType());
	}

}
