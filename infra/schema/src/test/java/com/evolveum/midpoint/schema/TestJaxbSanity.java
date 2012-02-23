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
import java.io.IOException;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

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
	public void testUnmarshallAndEqualsUser() throws JAXBException {
		System.out.println("\n\n ===[testUnmarshallAndEqualsUser]===\n");
		
		// GIVEN
		JAXBElement<UserType> userEl1 = PrismTestUtil.unmarshalElement(new File(USER_BARBOSSA_FILENAME),UserType.class);
		UserType user1 = userEl1.getValue();
		assertNotNull(user1);
		
		JAXBElement<UserType> userEl2 = PrismTestUtil.unmarshalElement(new File(USER_BARBOSSA_FILENAME),UserType.class);
		UserType user2 = userEl2.getValue();
		assertNotNull(user2);
		
		// Compare plain JAXB objects (not backed by containers)
		AccountConstructionType ac1 = user1.getAssignment().get(0).getAccountConstruction();
		AccountConstructionType ac2 = user2.getAssignment().get(0).getAccountConstruction();
		assertTrue("AccountConstructionType not equals", ac1.equals(ac2));
		
		// WHEN, THEN
		assertTrue("User not equals", user1.equals(user2));
		
		assertTrue("HashCode does not match", user1.hashCode() == user2.hashCode());
	}

	@Test
	public void testUnmarshallAndEqualsResource() throws JAXBException {
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
	public void testAssignmentEquals() throws JAXBException {
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
