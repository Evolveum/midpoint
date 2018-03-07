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

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.util.JaxbTestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.testng.AssertJUnit;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectModificationType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;
import com.evolveum.prism.xml.ns._public.types_3.SchemaDefinitionType;

/**
 * @author semancik
 *
 */
@Deprecated
public class TestJaxbSanity {

	public static final String TEST_DIR = "src/test/resources/common";
	public static final String USER_BARBOSSA_FILENAME = TEST_DIR + "/user-barbossa.xml";
	public static final String RESOURCE_OPENDJ_FILENAME = TEST_DIR + "/resource-opendj.xml";

	@BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}

	@Test
	public void testGeneratedEquals() throws JAXBException {
		System.out.println("\n\n ===[ testGeneratedEquals ]===\n");

		assertHasEquals(ObjectType.class);
		assertHasEquals(AssignmentType.class);
		assertHasEquals(MappingType.class);
		assertHasEquals(ProtectedStringType.class);

        assertHasHashCode(ObjectType.class);
        assertHasHashCode(AssignmentType.class);
        assertHasHashCode(MappingType.class);
        assertHasHashCode(ProtectedStringType.class);
	}

	private void assertHasEquals(Class<?> clazz) {
		try {
			Method method = clazz.getDeclaredMethod("equals", Object.class);
			assertNotNull("No equals method in "+clazz.getSimpleName(), method);
		} catch (SecurityException e) {
			AssertJUnit.fail("No equals method in "+clazz.getSimpleName());
		} catch (NoSuchMethodException e) {
			AssertJUnit.fail("No equals method in "+clazz.getSimpleName());
		}
	}

	private void assertHasHashCode(Class<?> clazz) {
		try {
			Method method = clazz.getDeclaredMethod("hashCode");
			assertNotNull("No hashCode method in "+clazz.getSimpleName(), method);
		} catch (SecurityException e) {
			AssertJUnit.fail("No hashCode method in "+clazz.getSimpleName());
		} catch (NoSuchMethodException e) {
			AssertJUnit.fail("No hashCode method in "+clazz.getSimpleName());
		}
	}

	@Test
	public void testUnmarshallAndEqualsUserJaxb() throws Exception {
		System.out.println("\n\n ===[ testUnmarshallAndEqualsUserJaxb ]===\n");

		// GIVEN
		JAXBElement<UserType> userEl1 = JaxbTestUtil.getInstance().unmarshalElement(new File(USER_BARBOSSA_FILENAME),UserType.class);
		UserType user1Type = userEl1.getValue();
		assertNotNull(user1Type);
		PrismObject<UserType> user1 = user1Type.asPrismObject();

		JAXBElement<UserType> userEl2 = JaxbTestUtil.getInstance().unmarshalElement(new File(USER_BARBOSSA_FILENAME),UserType.class);
		UserType user2Type = userEl2.getValue();
		assertNotNull(user2Type);
		PrismObject<UserType> user2 = user2Type.asPrismObject();

		// Compare plain JAXB objects (not backed by containers)
		ConstructionType ac1 = user1Type.getAssignment().get(0).getConstruction();
		ConstructionType ac2 = user2Type.getAssignment().get(0).getConstruction();
		assertTrue("ConstructionType not equals", ac1.equals(ac2));

		// WHEN, THEN
		ObjectDelta<UserType> objectDelta = user1.diff(user2);
		System.out.println("User delta:");
		System.out.println(objectDelta.debugDump());
		assertTrue("User delta is not empty", objectDelta.isEmpty());

		assertTrue("User not equals (PrismObject)", user1.equals(user2));
		assertTrue("User not equivalent (PrismObject)", user1.equivalent(user2));
		assertTrue("User not equals (Objectable)", user1Type.equals(user2Type));

		assertTrue("HashCode does not match (PrismObject)", user1.hashCode() == user2.hashCode());
		assertTrue("HashCode does not match (Objectable)", user1Type.hashCode() == user2Type.hashCode());
	}

	@Test
	public void testUnmarshallAndEqualsUserPrism() throws Exception {
		System.out.println("\n\n ===[testUnmarshallAndEqualsUserPrism]===\n");

		// GIVEN
		PrismObject<UserType> user1 = PrismTestUtil.parseObject(new File(USER_BARBOSSA_FILENAME));
		UserType user1Type = user1.asObjectable();

		PrismObject<UserType> user2 = PrismTestUtil.parseObject(new File(USER_BARBOSSA_FILENAME));
		UserType user2Type = user2.asObjectable();

		// Compare plain JAXB objects (not backed by containers)
		ConstructionType ac1 = user1Type.getAssignment().get(0).getConstruction();
		ConstructionType ac2 = user2Type.getAssignment().get(0).getConstruction();
		assertTrue("ConstructionType not equals (JAXB)", ac1.equals(ac2));
		assertTrue("ConstructionType hashcode does not match (JAXB)", ac1.hashCode() == ac2.hashCode());

		AssignmentType as1Type = user1Type.getAssignment().get(0);
		PrismContainerValue<AssignmentType> as1ContVal = as1Type.asPrismContainerValue();
		PrismContainer<AssignmentType> as1Cont = as1ContVal.getContainer();
		AssignmentType as2Type = user2Type.getAssignment().get(0);
		PrismContainerValue<AssignmentType> as2ContVal = as2Type.asPrismContainerValue();
		PrismContainer<AssignmentType> as2Cont = as2ContVal.getContainer();
		assertTrue("Assignment not equals (ContainerValue)", as1ContVal.equals(as2ContVal));
		assertTrue("Assignment not equals (ContainerValue, ignoreMetadata)", as1ContVal.equals(as2ContVal,true));
		assertTrue("Assignment not equals (ContainerValue, not ignoreMetadata)", as1ContVal.equals(as2ContVal,false));
		assertTrue("Assignment not equivalent (ContainerValue)", as1ContVal.equivalent(as2ContVal));
		assertTrue("Assignment not equals (Container)", as1Cont.equals(as2Cont));
		assertTrue("Assignment not equivalent (Container)", as1Cont.equivalent(as2Cont));
		assertTrue("AssignmentType not equals (JAXB)", as1Type.equals(as2Type));
		assertTrue("Assignment hashcode does not match (Container)", as1Cont.hashCode() == as2Cont.hashCode());
		assertTrue("Assignment hashcode does not match (Objectable)", as1Type.hashCode() == as2Type.hashCode());

		// Compare object inner value
		assertTrue("User prism values do not match", user1.getValue().equals(user2.getValue()));

		// WHEN, THEN
		ObjectDelta<UserType> objectDelta = user1.diff(user1);
		System.out.println("User delta:");
		System.out.println(objectDelta.debugDump());
		assertTrue("User delta is not empty", objectDelta.isEmpty());

		assertTrue("User not equals (PrismObject)", user1.equals(user2));
		assertTrue("User not equivalent (PrismObject)", user1.equivalent(user2));
		assertTrue("User not equals (Objectable)", user1Type.equals(user2Type));

		assertTrue("User hashcode does not match (PrismObject)", user1.hashCode() == user2.hashCode());
		assertTrue("User hashcode does not match (Objectable)", user1Type.hashCode() == user2Type.hashCode());
	}

	@Test
	public void testUnmarshallAndEqualsUserMixed() throws Exception {
		System.out.println("\n\n ===[testUnmarshallAndEqualsUserMixed]===\n");

		// GIVEN
		PrismObject<UserType> user1 = PrismTestUtil.parseObject(new File(USER_BARBOSSA_FILENAME));
		UserType user1Type = user1.asObjectable();

		JAXBElement<UserType> userEl2 = JaxbTestUtil.getInstance().unmarshalElement(new File(USER_BARBOSSA_FILENAME),UserType.class);
		UserType user2Type = userEl2.getValue();
		assertNotNull(user2Type);
		PrismObject<UserType> user2 = user2Type.asPrismObject();

		// Compare plain JAXB objects (not backed by containers)
		ConstructionType ac1 = user1Type.getAssignment().get(0).getConstruction();
		ConstructionType ac2 = user2Type.getAssignment().get(0).getConstruction();
		assertTrue("ConstructionType not equals", ac1.equals(ac2));
		System.out.println(user1.debugDump());
		System.out.println(user2.debugDump());
		// WHEN, THEN
		assertTrue("User not equals (PrismObject)", user1.equals(user2));
		assertTrue("User not equivalent (PrismObject)", user1.equivalent(user2));
		assertTrue("User not equals (Objectable)", user1Type.equals(user2Type));

		assertTrue("HashCode does not match (PrismObject)", user1.hashCode() == user2.hashCode());
		assertTrue("HashCode does not match (Objectable)", user1Type.hashCode() == user2Type.hashCode());
	}


	@Test
	public void testUnmarshallAndEqualsResourceSchema() throws JAXBException, SchemaException, FileNotFoundException {
		System.out.println("\n\n ===[testUnmarshallAndEqualsResourceSchema]===\n");

		// GIVEN
		ResourceType resource1Type = JaxbTestUtil.getInstance().unmarshalObject(new File(RESOURCE_OPENDJ_FILENAME), ResourceType.class);
		assertNotNull(resource1Type);
		SchemaDefinitionType schemaDefinition1 = resource1Type.getSchema().getDefinition();

		ResourceType resource2Type = JaxbTestUtil.getInstance().unmarshalObject(new File(RESOURCE_OPENDJ_FILENAME), ResourceType.class);
		assertNotNull(resource2Type);
		SchemaDefinitionType schemaDefinition2 = resource2Type.getSchema().getDefinition();

		// WHEN
		boolean equals = schemaDefinition1.equals(schemaDefinition2);

		// THEN
		assertTrue("Schema definition not equal", equals);

		assertEquals("Hashcode does not match", schemaDefinition1.hashCode(), schemaDefinition2.hashCode());
	}

	@Test
	public void testUnmarshallAndEqualsResource() throws JAXBException, SchemaException, FileNotFoundException {
		System.out.println("\n\n ===[testUnmarshallAndEqualsResource]===\n");

		// GIVEN
		ResourceType resource1Type = JaxbTestUtil.getInstance().unmarshalObject(new File(RESOURCE_OPENDJ_FILENAME), ResourceType.class);
		assertNotNull(resource1Type);
		System.out.println("Resource1 " + resource1Type.asPrismObject().debugDump());
		PrismObject resource1 = resource1Type.asPrismObject();

		ResourceType resource2Type = JaxbTestUtil.getInstance().unmarshalObject(new File(RESOURCE_OPENDJ_FILENAME), ResourceType.class);
		assertNotNull(resource2Type);
		System.out.println("Resource2 " + resource2Type.asPrismObject().debugDump());
		PrismObject resource2 = resource2Type.asPrismObject();

		// WHEN, THEN
		ObjectDelta<ResourceType> objectDelta = resource1.diff(resource2);
		System.out.println("Resource delta:");
		System.out.println(objectDelta.debugDump());
		assertTrue("Resource delta is not empty", objectDelta.isEmpty());

		assertTrue("Resource not equal", resource1Type.equals(resource2Type));

		System.out.println("HASH");
		System.out.println(resource1Type.hashCode());
		System.out.println(resource2Type.hashCode());
		assertTrue("Resource hashcode does not match", resource1Type.hashCode() == resource2Type.hashCode());

		PrismPropertyValue<Object> pv1 = new PrismPropertyValue<>(resource1Type.getConnectorConfiguration());
		PrismPropertyValue<Object> pv2 = new PrismPropertyValue<>(resource2Type.getConnectorConfiguration());

		assertTrue("Real property values not equal",pv1.equalsRealValue(pv2));
	}

	@Test
	public void testAssignmentEquals() throws JAXBException, SchemaException, FileNotFoundException {
		System.out.println("\n\n ===[testAssnignmentEquals]===\n");

		// GIVEN
		JAXBElement<UserType> userEl1 = JaxbTestUtil.getInstance().unmarshalElement(new File(USER_BARBOSSA_FILENAME), UserType.class);
		UserType user = userEl1.getValue();
		assertNotNull(user);

		AssignmentType userAssignmentType = user.getAssignment().get(0);
		assertNotNull(userAssignmentType);

		System.out.println("\n*** user assignment");
		System.out.println(PrismTestUtil.serializeAnyDataWrapped(userAssignmentType));

		JAXBElement<ObjectModificationType> modEl = JaxbTestUtil.getInstance().unmarshalElement(new File(TEST_DIR, "user-barbossa-modify-delete-assignment-account-opendj-attr.xml"),ObjectModificationType.class);
		ObjectModificationType mod = modEl.getValue();
		assertNotNull(mod);

		//FIXME : modification value -> rawType...
        RawType rawType = mod.getItemDelta().get(0).getValue().get(0);
        ItemDefinition assignmentDefinition = PrismTestUtil.getPrismContext().getSchemaRegistry().findContainerDefinitionByCompileTimeClass(AssignmentType.class);
        assertNotNull(assignmentDefinition);
		AssignmentType assignmentType = ((PrismContainerValue<AssignmentType>) rawType.getParsedValue(assignmentDefinition, null)).getValue();
//                was: (JAXBElement<AssignmentType>) mod.getItemDelta().get(0).getValue().get(0).getContent().get(0);
		assertNotNull(assignmentType);

		System.out.println("\n*** assignment");
		System.out.println(PrismTestUtil.serializeAnyDataWrapped(assignmentType));

		// WHEN, THEN

		assertTrue("Assignment not equals", userAssignmentType.equals(assignmentType));

		assertTrue("HashCode does not match", userAssignmentType.hashCode() == assignmentType.hashCode());
	}

    @Test
    public void testObjectReferenceNullSet() throws Exception {
        System.out.println("\n\n ===[testObjectReferenceNullSet]===\n");

        //GIVEN
        SystemConfigurationType config = new SystemConfigurationType();
        PrismTestUtil.getPrismContext().adopt(config);

        //WHEN
        config.setGlobalPasswordPolicyRef(null);

        //THEN
        SystemConfigurationType configNew = new SystemConfigurationType();

        ObjectReferenceType ref = new ObjectReferenceType();
        ref.setOid("1234");
        ref.setType(ValuePolicyType.COMPLEX_TYPE);

        configNew.setGlobalPasswordPolicyRef(ref);
        configNew.setGlobalPasswordPolicyRef(null);

        PrismTestUtil.getPrismContext().adopt(configNew);

        assertTrue(config.equals(configNew));
    }
}
