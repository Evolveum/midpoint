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

import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertNotNull;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

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
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ProtectedStringType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ValueConstructionType;

/**
 * @author semancik
 *
 */
public class TestJaxbSanity {

	public static final String TEST_DIR = "src/test/resources/schema";
	public static final String USER_BARBOSSA_FILENAME = TEST_DIR + "/user-barbossa.xml";
	public static final String RESOURCE_OPENDJ_FILENAME = TEST_DIR + "/resource-opendj.xml";
	
	@BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		DebugUtil.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(new MidPointPrismContextFactory());
	}
	
	@Test
	public void testGeneratedEquals() throws JAXBException {
		System.out.println("\n\n ===[ testGeneratedEquals ]===\n");
		
		assertHasEquals(UserType.class);
		assertHasEquals(AccountShadowType.class);
		assertHasEquals(AssignmentType.class);
		assertHasEquals(ValueConstructionType.class);
		assertHasEquals(ProtectedStringType.class);
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

	@Test
	public void testUnmarshallAndEqualsUserJaxb() throws JAXBException, SchemaException, FileNotFoundException {
		System.out.println("\n\n ===[ testUnmarshallAndEqualsUserJaxb ]===\n");
		
		// GIVEN
		JAXBElement<UserType> userEl1 = PrismTestUtil.unmarshalElement(new File(USER_BARBOSSA_FILENAME),UserType.class);
		UserType user1Type = userEl1.getValue();
		assertNotNull(user1Type);
		PrismObject<UserType> user1 = user1Type.asPrismObject();
		
		JAXBElement<UserType> userEl2 = PrismTestUtil.unmarshalElement(new File(USER_BARBOSSA_FILENAME),UserType.class);
		UserType user2Type = userEl2.getValue();
		assertNotNull(user2Type);
		PrismObject<UserType> user2 = user2Type.asPrismObject();
		
		// Compare plain JAXB objects (not backed by containers)
		AccountConstructionType ac1 = user1Type.getAssignment().get(0).getAccountConstruction();
		AccountConstructionType ac2 = user2Type.getAssignment().get(0).getAccountConstruction();
		assertTrue("AccountConstructionType not equals", ac1.equals(ac2));
		
		// WHEN, THEN
		ObjectDelta<UserType> objectDelta = user1.diff(user1);
		System.out.println("User delta:");
		System.out.println(objectDelta.dump());
		assertTrue("User delta is not empty", objectDelta.isEmpty());
		
		assertTrue("User not equals (PrismObject)", user1.equals(user2));
		assertTrue("User not equivalent (PrismObject)", user1.equivalent(user2));
		assertTrue("User not equals (Objectable)", user1Type.equals(user2Type));
		
		assertTrue("HashCode does not match (PrismObject)", user1.hashCode() == user2.hashCode());
		assertTrue("HashCode does not match (Objectable)", user1Type.hashCode() == user2Type.hashCode());
	}
	
	@Test
	public void testUnmarshallAndEqualsUserPrism() throws SchemaException {
		System.out.println("\n\n ===[testUnmarshallAndEqualsUserPrism]===\n");
		
		// GIVEN
		PrismObject<UserType> user1 = PrismTestUtil.parseObject(new File(USER_BARBOSSA_FILENAME));
		UserType user1Type = user1.asObjectable();
		
		PrismObject<UserType> user2 = PrismTestUtil.parseObject(new File(USER_BARBOSSA_FILENAME));
		UserType user2Type = user2.asObjectable();
		
		// Compare plain JAXB objects (not backed by containers)
		AccountConstructionType ac1 = user1Type.getAssignment().get(0).getAccountConstruction();
		AccountConstructionType ac2 = user2Type.getAssignment().get(0).getAccountConstruction();
		assertTrue("AccountConstructionType not equals (JAXB)", ac1.equals(ac2));
		assertTrue("AccountConstructionType hashcode does not match (JAXB)", ac1.hashCode() == ac2.hashCode());
		
		AssignmentType as1Type = user1Type.getAssignment().get(0);
		PrismContainer as1Cont = as1Type.asPrismContainer();
		PrismContainerValue as1ContVal = as1Type.asPrismContainerValue();
		AssignmentType as2Type = user2Type.getAssignment().get(0);
		PrismContainer as2Cont = as2Type.asPrismContainer();
		PrismContainerValue as2ContVal = as2Type.asPrismContainerValue();
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
		System.out.println(objectDelta.dump());
		assertTrue("User delta is not empty", objectDelta.isEmpty());
		
		assertTrue("User not equals (PrismObject)", user1.equals(user2));
		assertTrue("User not equivalent (PrismObject)", user1.equivalent(user2));
		assertTrue("User not equals (Objectable)", user1Type.equals(user2Type));
		
		assertTrue("User hashcode does not match (PrismObject)", user1.hashCode() == user2.hashCode());
		assertTrue("User hashcode does not match (Objectable)", user1Type.hashCode() == user2Type.hashCode());
	}

	@Test
	public void testUnmarshallAndEqualsUserMixed() throws SchemaException, JAXBException, FileNotFoundException {
		System.out.println("\n\n ===[testUnmarshallAndEqualsUserMixed]===\n");
		
		// GIVEN
		PrismObject<UserType> user1 = PrismTestUtil.parseObject(new File(USER_BARBOSSA_FILENAME));
		UserType user1Type = user1.asObjectable();
		
		JAXBElement<UserType> userEl2 = PrismTestUtil.unmarshalElement(new File(USER_BARBOSSA_FILENAME),UserType.class);
		UserType user2Type = userEl2.getValue();
		assertNotNull(user2Type);
		PrismObject<UserType> user2 = user2Type.asPrismObject();
		
		// Compare plain JAXB objects (not backed by containers)
		AccountConstructionType ac1 = user1Type.getAssignment().get(0).getAccountConstruction();
		AccountConstructionType ac2 = user2Type.getAssignment().get(0).getAccountConstruction();
		assertTrue("AccountConstructionType not equals", ac1.equals(ac2));
		
		// WHEN, THEN
		assertTrue("User not equals (PrismObject)", user1.equals(user2));
		assertTrue("User not equivalent (PrismObject)", user1.equivalent(user2));
		assertTrue("User not equals (Objectable)", user1Type.equals(user2Type));
		
		assertTrue("HashCode does not match (PrismObject)", user1.hashCode() == user2.hashCode());
		assertTrue("HashCode does not match (Objectable)", user1Type.hashCode() == user2Type.hashCode());
	}

	
	@Test
	public void testUnmarshallAndEqualsResource() throws JAXBException, SchemaException, FileNotFoundException {
		System.out.println("\n\n ===[testUnmarshallAndEqualsResource]===\n");
		
		// GIVEN
		JAXBElement<ResourceType> resourceEl1 = PrismTestUtil.unmarshalElement(new File(RESOURCE_OPENDJ_FILENAME),ResourceType.class);
		ResourceType resource1 = resourceEl1.getValue();
		assertNotNull(resource1);
		
		JAXBElement<ResourceType> resourceEl2 = PrismTestUtil.unmarshalElement(new File(RESOURCE_OPENDJ_FILENAME),ResourceType.class);
		ResourceType resource2 = resourceEl2.getValue();
		assertNotNull(resource2);
		
		// WHEN, THEN
		assertTrue("Resource not equal", resource1.equals(resource2));
		
		assertTrue("HashCode does not match", resource1.hashCode() == resource2.hashCode());
		
		PrismPropertyValue<Object> pv1 = new PrismPropertyValue<Object>(resource1.getConfiguration());
		PrismPropertyValue<Object> pv2 = new PrismPropertyValue<Object>(resource2.getConfiguration());
		
		assertTrue("Real property values not equal",pv1.equalsRealValue(pv2));
	}

	@Test
	public void testAssignmentEquals() throws JAXBException, SchemaException, FileNotFoundException {
		System.out.println("\n\n ===[testAssnignmentEquals]===\n");
		
		// GIVEN
		JAXBElement<UserType> userEl1 = PrismTestUtil.unmarshalElement(new File(USER_BARBOSSA_FILENAME),UserType.class);
		UserType user = userEl1.getValue();
		assertNotNull(user);
		
		AssignmentType userAssignmentType = user.getAssignment().get(0);
		assertNotNull(userAssignmentType);

		System.out.println("\n*** user assignment");
		System.out.println(PrismTestUtil.marshalWrap(userAssignmentType));

		JAXBElement<ObjectModificationType> modEl = PrismTestUtil.unmarshalElement(new File(TEST_DIR, "user-barbossa-modify-delete-assignment-account-opendj-attr.xml"),ObjectModificationType.class);
		ObjectModificationType mod = modEl.getValue();
		assertNotNull(mod);
		
		JAXBElement<AssignmentType> assignmentTypeEl = (JAXBElement<AssignmentType>) mod.getPropertyModification().get(0).getValue().getAny().get(0);
		AssignmentType assignmentType = assignmentTypeEl.getValue();
		assertNotNull(assignmentType);
		
		System.out.println("\n*** assignment");
		System.out.println(PrismTestUtil.marshalWrap(assignmentType));
		
		// WHEN, THEN
		
		assertTrue("Assignment not equals", userAssignmentType.equals(assignmentType));
		
		assertTrue("HashCode does not match", userAssignmentType.hashCode() == assignmentType.hashCode());
	}
	
}
