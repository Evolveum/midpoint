/**
 * Copyright (c) 2012 Evolveum
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
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */
package com.evolveum.midpoint.prism;

import static com.evolveum.midpoint.prism.PrismInternalTestUtil.*;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.IOException;
import java.util.Collection;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.foo.ActivationType;
import com.evolveum.midpoint.prism.foo.AssignmentType;
import com.evolveum.midpoint.prism.foo.UserType;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author semancik
 *
 */
public class TestDelta {
	
	private static final String USER_FOO_OID = "01234567";

	@BeforeSuite
	public void setupDebug() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(DEFAULT_NAMESPACE_PREFIX);
		PrismTestUtil.resetPrismContext(new PrismInternalTestUtil());
	}
	
	@Test
    public void testPropertyDeltaMerge01() throws Exception {
		System.out.println("\n\n===[ testPropertyDeltaMerge01 ]===\n");
		
		// GIVEN
		PrismPropertyDefinition propertyDefinition = new PrismPropertyDefinition(UserType.F_DESCRIPTION, 
				UserType.F_DESCRIPTION, DOMUtil.XSD_STRING, PrismTestUtil.getPrismContext());

		PropertyDelta<String> delta1 = new PropertyDelta<String>(propertyDefinition);
		delta1.addValueToAdd(new PrismPropertyValue<String>("add1"));

		PropertyDelta<String> delta2 = new PropertyDelta<String>(propertyDefinition);
		delta2.addValueToAdd(new PrismPropertyValue<String>("add2"));
		
		// WHEN
		delta1.merge(delta2);
		
		// THEN
		System.out.println("Merged delta:");
		System.out.println(delta1.dump());

		PrismAsserts.assertNoReplace(delta1);
		PrismAsserts.assertAdd(delta1, "add1", "add2");
		PrismAsserts.assertNoDelete(delta1);
	}

	@Test
    public void testPropertyDeltaMerge02() throws Exception {
		System.out.println("\n\n===[ testPropertyDeltaMerge02 ]===\n");
		
		// GIVEN
		PrismPropertyDefinition propertyDefinition = new PrismPropertyDefinition(UserType.F_DESCRIPTION, 
				UserType.F_DESCRIPTION, DOMUtil.XSD_STRING, PrismTestUtil.getPrismContext());

		PropertyDelta<String> delta1 = new PropertyDelta<String>(propertyDefinition);
		delta1.addValueToDelete(new PrismPropertyValue<String>("del1"));

		PropertyDelta<String> delta2 = new PropertyDelta<String>(propertyDefinition);
		delta2.addValueToDelete(new PrismPropertyValue<String>("del2"));
		
		// WHEN
		delta1.merge(delta2);
		
		// THEN
		System.out.println("Merged delta:");
		System.out.println(delta1.dump());

		PrismAsserts.assertNoReplace(delta1);
		PrismAsserts.assertNoAdd(delta1);
		PrismAsserts.assertDelete(delta1, "del1", "del2");
	}

	@Test
    public void testPropertyDeltaMerge03() throws Exception {
		System.out.println("\n\n===[ testPropertyDeltaMerge03 ]===\n");
		
		// GIVEN
		PrismPropertyDefinition propertyDefinition = new PrismPropertyDefinition(UserType.F_DESCRIPTION, 
				UserType.F_DESCRIPTION, DOMUtil.XSD_STRING, PrismTestUtil.getPrismContext());

		PropertyDelta<String> delta1 = new PropertyDelta<String>(propertyDefinition);
		delta1.addValueToAdd(new PrismPropertyValue<String>("add1"));
		delta1.addValueToDelete(new PrismPropertyValue<String>("del1"));

		PropertyDelta<String> delta2 = new PropertyDelta<String>(propertyDefinition);
		delta2.addValueToAdd(new PrismPropertyValue<String>("add2"));
		delta2.addValueToDelete(new PrismPropertyValue<String>("del2"));
		
		// WHEN
		delta1.merge(delta2);
		
		// THEN
		System.out.println("Merged delta:");
		System.out.println(delta1.dump());

		PrismAsserts.assertNoReplace(delta1);
		PrismAsserts.assertAdd(delta1, "add1", "add2");
		PrismAsserts.assertDelete(delta1, "del1", "del2");
	}

	@Test
    public void testPropertyDeltaMerge04() throws Exception {
		System.out.println("\n\n===[ testPropertyDeltaMerge04 ]===\n");
		
		// GIVEN
		PrismPropertyDefinition propertyDefinition = new PrismPropertyDefinition(UserType.F_DESCRIPTION, 
				UserType.F_DESCRIPTION, DOMUtil.XSD_STRING, PrismTestUtil.getPrismContext());

		PropertyDelta<String> delta1 = new PropertyDelta<String>(propertyDefinition);
		delta1.addValueToAdd(new PrismPropertyValue<String>("add1"));
		delta1.addValueToDelete(new PrismPropertyValue<String>("del1"));

		PropertyDelta<String> delta2 = new PropertyDelta<String>(propertyDefinition);
		delta2.addValueToAdd(new PrismPropertyValue<String>("add2"));
		delta2.addValueToDelete(new PrismPropertyValue<String>("add1"));
		
		// WHEN
		delta1.merge(delta2);
		
		// THEN
		System.out.println("Merged delta:");
		System.out.println(delta1.dump());

		PrismAsserts.assertNoReplace(delta1);
		PrismAsserts.assertAdd(delta1, "add2");
		PrismAsserts.assertDelete(delta1, "del1");
	}
	
	@Test
    public void testPropertyDeltaMerge05() throws Exception {
		System.out.println("\n\n===[ testPropertyDeltaMerge05 ]===\n");
		
		// GIVEN
		PrismPropertyDefinition propertyDefinition = new PrismPropertyDefinition(UserType.F_DESCRIPTION, 
				UserType.F_DESCRIPTION, DOMUtil.XSD_STRING, PrismTestUtil.getPrismContext());

		PropertyDelta<String> delta1 = new PropertyDelta<String>(propertyDefinition);
		delta1.addValueToAdd(new PrismPropertyValue<String>("add1"));

		PropertyDelta<String> delta2 = new PropertyDelta<String>(propertyDefinition);
		delta2.addValueToAdd(new PrismPropertyValue<String>("add2"));
		delta2.addValueToDelete(new PrismPropertyValue<String>("add1"));
		
		// WHEN
		delta1.merge(delta2);
		
		// THEN
		System.out.println("Merged delta:");
		System.out.println(delta1.dump());

		PrismAsserts.assertNoReplace(delta1);
		PrismAsserts.assertAdd(delta1, "add2");
		PrismAsserts.assertNoDelete(delta1);
	}
	
	@Test
    public void testPropertyDeltaMerge06() throws Exception {
		System.out.println("\n\n===[ testPropertyDeltaMerge06 ]===\n");
		
		// GIVEN
		PrismPropertyDefinition propertyDefinition = new PrismPropertyDefinition(UserType.F_DESCRIPTION, 
				UserType.F_DESCRIPTION, DOMUtil.XSD_STRING, PrismTestUtil.getPrismContext());

		PropertyDelta<String> delta1 = new PropertyDelta<String>(propertyDefinition);
		delta1.addValueToAdd(new PrismPropertyValue<String>("add1"));
		delta1.addValueToDelete(new PrismPropertyValue<String>("del1"));

		PropertyDelta<String> delta2 = new PropertyDelta<String>(propertyDefinition);
		delta2.addValueToAdd(new PrismPropertyValue<String>("del1"));
		
		// WHEN
		delta1.merge(delta2);
		
		// THEN
		System.out.println("Merged delta:");
		System.out.println(delta1.dump());

		PrismAsserts.assertNoReplace(delta1);
		PrismAsserts.assertAdd(delta1, "add1");
		PrismAsserts.assertNoDelete(delta1);
	}
	
	@Test
    public void testPropertyDeltaMerge10() throws Exception {
		System.out.println("\n\n===[ testPropertyDeltaMerge10 ]===\n");
		
		// GIVEN
		PrismPropertyDefinition propertyDefinition = new PrismPropertyDefinition(UserType.F_DESCRIPTION, 
				UserType.F_DESCRIPTION, DOMUtil.XSD_STRING, PrismTestUtil.getPrismContext());

		PropertyDelta<String> delta1 = new PropertyDelta<String>(propertyDefinition);
		delta1.setValuesToReplace(new PrismPropertyValue<String>("r1x"), new PrismPropertyValue<String>("r1y"));

		PropertyDelta<String> delta2 = new PropertyDelta<String>(propertyDefinition);
		delta2.addValueToAdd(new PrismPropertyValue<String>("add2"));
		
		// WHEN
		delta1.merge(delta2);
		
		// THEN
		System.out.println("Merged delta:");
		System.out.println(delta1.dump());

		PrismAsserts.assertReplace(delta1, "r1x", "r1y", "add2");
		PrismAsserts.assertNoAdd(delta1);
		PrismAsserts.assertNoDelete(delta1);
	}
	
	@Test
    public void testPropertyDeltaMerge11() throws Exception {
		System.out.println("\n\n===[ testPropertyDeltaMerge11 ]===\n");
		
		// GIVEN
		PrismPropertyDefinition propertyDefinition = new PrismPropertyDefinition(UserType.F_DESCRIPTION, 
				UserType.F_DESCRIPTION, DOMUtil.XSD_STRING, PrismTestUtil.getPrismContext());

		PropertyDelta<String> delta1 = new PropertyDelta<String>(propertyDefinition);
		delta1.setValuesToReplace(new PrismPropertyValue<String>("r1x"), new PrismPropertyValue<String>("r1y"));

		PropertyDelta<String> delta2 = new PropertyDelta<String>(propertyDefinition);
		delta2.addValueToAdd(new PrismPropertyValue<String>("add2"));
		delta2.addValueToDelete(new PrismPropertyValue<String>("r1y"));
		
		// WHEN
		delta1.merge(delta2);
		
		// THEN
		System.out.println("Merged delta:");
		System.out.println(delta1.dump());

		PrismAsserts.assertReplace(delta1, "r1x", "add2");
		PrismAsserts.assertNoAdd(delta1);
		PrismAsserts.assertNoDelete(delta1);
	}

	@Test
    public void testPropertyDeltaMerge12() throws Exception {
		System.out.println("\n\n===[ testPropertyDeltaMerge12 ]===\n");
		
		// GIVEN
		PrismPropertyDefinition propertyDefinition = new PrismPropertyDefinition(UserType.F_DESCRIPTION, 
				UserType.F_DESCRIPTION, DOMUtil.XSD_STRING, PrismTestUtil.getPrismContext());

		PropertyDelta<String> delta1 = new PropertyDelta<String>(propertyDefinition);
		delta1.setValuesToReplace(new PrismPropertyValue<String>("r1x"), new PrismPropertyValue<String>("r1y"));

		PropertyDelta<String> delta2 = new PropertyDelta<String>(propertyDefinition);
		delta2.addValueToAdd(new PrismPropertyValue<String>("add2"));
		delta2.addValueToDelete(new PrismPropertyValue<String>("del2"));
		
		// WHEN
		delta1.merge(delta2);
		
		// THEN
		System.out.println("Merged delta:");
		System.out.println(delta1.dump());

		PrismAsserts.assertReplace(delta1, "r1x", "r1y", "add2");
		PrismAsserts.assertNoAdd(delta1);
		PrismAsserts.assertNoDelete(delta1);
	}

	@Test
    public void testPropertyDeltaMerge20() throws Exception {
		System.out.println("\n\n===[ testPropertyDeltaMerge20 ]===\n");
		
		// GIVEN
		PrismPropertyDefinition propertyDefinition = new PrismPropertyDefinition(UserType.F_DESCRIPTION, 
				UserType.F_DESCRIPTION, DOMUtil.XSD_STRING, PrismTestUtil.getPrismContext());

		PropertyDelta<String> delta1 = new PropertyDelta<String>(propertyDefinition);
		delta1.addValueToAdd(new PrismPropertyValue<String>("add1"));
		delta1.addValueToDelete(new PrismPropertyValue<String>("del1"));

		PropertyDelta<String> delta2 = new PropertyDelta<String>(propertyDefinition);
		delta2.setValuesToReplace(new PrismPropertyValue<String>("r2x"), new PrismPropertyValue<String>("r2y"));
		
		// WHEN
		delta1.merge(delta2);
		
		// THEN
		System.out.println("Merged delta:");
		System.out.println(delta1.dump());

		PrismAsserts.assertReplace(delta1, "r2x", "r2y");
		PrismAsserts.assertNoAdd(delta1);
		PrismAsserts.assertNoDelete(delta1);
	}
	
	@Test
    public void testPropertyDeltaSwallow01() throws Exception {
		System.out.println("\n\n===[ testPropertyDeltaSwallow01 ]===\n");
		
		// GIVEN
		PrismPropertyDefinition propertyDefinition = new PrismPropertyDefinition(UserType.F_DESCRIPTION, 
				UserType.F_DESCRIPTION, DOMUtil.XSD_STRING, PrismTestUtil.getPrismContext());

		PropertyDelta<String> delta1 = new PropertyDelta<String>(propertyDefinition);
		delta1.addValueToAdd(new PrismPropertyValue<String>("add1"));
		ObjectDelta<UserType> objectDelta = new ObjectDelta<UserType>(UserType.class, ChangeType.MODIFY, 
				PrismTestUtil.getPrismContext());
		objectDelta.addModification(delta1);

		PropertyDelta<String> delta2 = new PropertyDelta<String>(propertyDefinition);
		delta2.addValueToAdd(new PrismPropertyValue<String>("add2"));
		
		// WHEN
		objectDelta.swallow(delta2);
		
		// THEN
		System.out.println("Swallowed delta:");
		System.out.println(objectDelta.dump());

		PrismAsserts.assertModifications(objectDelta, 1);
		PropertyDelta<String> modification = (PropertyDelta<String>) objectDelta.getModifications().iterator().next();
		PrismAsserts.assertNoReplace(modification);
		PrismAsserts.assertAdd(modification, "add1", "add2");
		PrismAsserts.assertNoDelete(modification);
	}
	
	@Test
    public void testSummarize01() throws Exception {
		System.out.println("\n\n===[ testSummarize01 ]===\n");
		
		// GIVEN
		PrismPropertyDefinition propertyDefinition = new PrismPropertyDefinition(UserType.F_DESCRIPTION, 
				UserType.F_DESCRIPTION, DOMUtil.XSD_STRING, PrismTestUtil.getPrismContext());

		PropertyDelta<String> delta1 = new PropertyDelta<String>(propertyDefinition);
		delta1.addValueToAdd(new PrismPropertyValue<String>("add1"));
		ObjectDelta<UserType> objectDelta1 = new ObjectDelta<UserType>(UserType.class, ChangeType.MODIFY, 
				PrismTestUtil.getPrismContext());
		objectDelta1.addModification(delta1);

		PropertyDelta<String> delta2 = new PropertyDelta<String>(propertyDefinition);
		delta2.addValueToAdd(new PrismPropertyValue<String>("add2"));
		ObjectDelta<UserType> objectDelta2 = new ObjectDelta<UserType>(UserType.class, ChangeType.MODIFY, 
				PrismTestUtil.getPrismContext());
		objectDelta2.addModification(delta2);
		
		// WHEN
		ObjectDelta<UserType> sumDelta = ObjectDelta.summarize(objectDelta1, objectDelta2);
		
		// THEN
		System.out.println("Summarized delta:");
		System.out.println(sumDelta.dump());

		PrismAsserts.assertModifications(sumDelta, 1);
		PropertyDelta<String> modification = (PropertyDelta<String>) sumDelta.getModifications().iterator().next();
		PrismAsserts.assertNoReplace(modification);
		PrismAsserts.assertAdd(modification, "add1", "add2");
		PrismAsserts.assertNoDelete(modification);
	}

	@Test
    public void testSummarize02() throws Exception {
		System.out.println("\n\n===[ testSummarize02 ]===\n");
		
		// GIVEN
		PrismPropertyDefinition propertyDefinition = new PrismPropertyDefinition(UserType.F_DESCRIPTION, 
				UserType.F_DESCRIPTION, DOMUtil.XSD_STRING, PrismTestUtil.getPrismContext());

		PropertyDelta<String> delta1 = new PropertyDelta<String>(propertyDefinition);
		delta1.addValueToDelete(new PrismPropertyValue<String>("del1"));
		ObjectDelta<UserType> objectDelta1 = new ObjectDelta<UserType>(UserType.class, ChangeType.MODIFY, 
				PrismTestUtil.getPrismContext());
		objectDelta1.addModification(delta1);

		PropertyDelta<String> delta2 = new PropertyDelta<String>(propertyDefinition);
		delta2.addValueToDelete(new PrismPropertyValue<String>("del2"));
		ObjectDelta<UserType> objectDelta2 = new ObjectDelta<UserType>(UserType.class, ChangeType.MODIFY, 
				PrismTestUtil.getPrismContext());
		objectDelta2.addModification(delta2);
		
		// WHEN
		ObjectDelta<UserType> sumDelta = ObjectDelta.summarize(objectDelta1, objectDelta2);
		
		// THEN
		System.out.println("Summarized delta:");
		System.out.println(sumDelta.dump());

		PrismAsserts.assertModifications(sumDelta, 1);
		PropertyDelta<String> modification = (PropertyDelta<String>) sumDelta.getModifications().iterator().next();
		PrismAsserts.assertNoReplace(modification);
		PrismAsserts.assertNoAdd(modification);
		PrismAsserts.assertDelete(modification, "del1", "del2");
	}
	
	@Test
    public void testSummarize05() throws Exception {
		System.out.println("\n\n===[ testSummarize05 ]===\n");
		
		// GIVEN
		PrismPropertyDefinition propertyDefinition = new PrismPropertyDefinition(UserType.F_DESCRIPTION, 
				UserType.F_DESCRIPTION, DOMUtil.XSD_STRING, PrismTestUtil.getPrismContext());

		PropertyDelta<String> delta1 = new PropertyDelta<String>(propertyDefinition);
		// Let's complicate the things a bit with origin. This should work even though origins do not match.
		delta1.addValueToAdd(new PrismPropertyValue<String>("add1", OriginType.OUTBOUND, null));
		ObjectDelta<UserType> objectDelta1 = new ObjectDelta<UserType>(UserType.class, ChangeType.MODIFY, 
				PrismTestUtil.getPrismContext());
		objectDelta1.addModification(delta1);

		PropertyDelta<String> delta2 = new PropertyDelta<String>(propertyDefinition);
		delta2.addValueToAdd(new PrismPropertyValue<String>("add2"));
		delta2.addValueToDelete(new PrismPropertyValue<String>("add1"));
		ObjectDelta<UserType> objectDelta2 = new ObjectDelta<UserType>(UserType.class, ChangeType.MODIFY, 
				PrismTestUtil.getPrismContext());
		objectDelta2.addModification(delta2);
		
		// WHEN
		ObjectDelta<UserType> sumDelta = ObjectDelta.summarize(objectDelta1, objectDelta2);
		
		// THEN
		System.out.println("Summarized delta:");
		System.out.println(sumDelta.dump());

		PrismAsserts.assertModifications(sumDelta, 1);
		PropertyDelta<String> modification = (PropertyDelta<String>) sumDelta.getModifications().iterator().next();
		PrismAsserts.assertNoReplace(modification);
		PrismAsserts.assertAdd(modification, "add2");
		PrismAsserts.assertNoDelete(modification);
	}

	
	@Test
    public void testAddPropertyMulti() throws Exception {
		System.out.println("\n\n===[ testAddPropertyMulti ]===\n");
		// GIVEN
		
		// User
		PrismObject<UserType> user = createUser();

		//Delta
    	ObjectDelta<UserType> userDelta = ObjectDelta.createModificationAddProperty(UserType.class, USER_FOO_OID, UserType.F_ADDITIONAL_NAMES, 
    			PrismTestUtil.getPrismContext(), PrismTestUtil.createPolyString("baz"));
				
		// WHEN
        userDelta.applyTo(user);
        
        // THEN
        assertEquals("Wrong OID", USER_FOO_OID, user.getOid());
        PrismAsserts.assertPropertyValue(user, UserType.F_ADDITIONAL_NAMES, PrismTestUtil.createPolyString("baz"), PrismTestUtil.createPolyString("foobar"));
        PrismContainer<AssignmentType> assignment = user.findContainer(UserType.F_ASSIGNMENT);
        assertNotNull("No assignment", assignment);
        assertEquals("Unexpected number of assignment values", 1, assignment.size());
    }
	
	@Test
    public void testAddAssignmentSameNullId() throws Exception {
		System.out.println("\n\n===[ testAddAssignmentSameNullId ]===\n");
		// GIVEN
		
		// User
		PrismObject<UserType> user = createUser();

		//Delta
    	PrismContainerValue<AssignmentType> assignmentValue = new PrismContainerValue<AssignmentType>();
    	// The value id is null
    	assignmentValue.setPropertyRealValue(AssignmentType.F_DESCRIPTION, "chamalalia patlama paprtala");
    	
		ObjectDelta<UserType> userDelta = ObjectDelta.createModificationAddContainer(UserType.class, USER_FOO_OID, 
				UserType.F_ASSIGNMENT, PrismTestUtil.getPrismContext(), assignmentValue);
				
		// WHEN
        userDelta.applyTo(user);
        
        // THEN
        System.out.println("User after delta application:");
        System.out.println(user.dump());
        assertEquals("Wrong OID", USER_FOO_OID, user.getOid());
        PrismAsserts.assertPropertyValue(user, UserType.F_ADDITIONAL_NAMES, PrismTestUtil.createPolyString("foobar"));
        PrismContainer<AssignmentType> assignment = user.findContainer(UserType.F_ASSIGNMENT);
        assertNotNull("No assignment", assignment);
        assertEquals("Unexpected number of assignment values", 1, assignment.size());
    }
	
	/**
	 * MODIFY/add + MODIFY/add
	 */
	@Test
    public void testObjectDeltaUnion01Simple() throws Exception {
		System.out.println("\n\n===[ testObjectDeltaUnion01Simple ]===\n");
		// GIVEN
		
		//Delta
    	ObjectDelta<UserType> userDelta1 = ObjectDelta.createModificationAddProperty(UserType.class, USER_FOO_OID, 
    			UserType.F_FULL_NAME, PrismTestUtil.getPrismContext(), PrismTestUtil.createPolyString("baz"));
    	ObjectDelta<UserType> userDelta2 = ObjectDelta.createModificationAddProperty(UserType.class, USER_FOO_OID, 
    			UserType.F_FULL_NAME, PrismTestUtil.getPrismContext(), PrismTestUtil.createPolyString("baz"));
				
		// WHEN
        ObjectDelta<UserType> userDeltaUnion = ObjectDelta.union(userDelta1, userDelta2);
        
        // THEN
        assertUnion01Delta(userDeltaUnion);
    }
	
	/**
	 * MODIFY/add + MODIFY/add
	 */	
	@Test
    public void testObjectDeltaUnion01Metadata() throws Exception {
		System.out.println("\n\n===[ testObjectDeltaUnion01Metadata ]===\n");
		// GIVEN
		
    	ObjectDelta<UserType> userDelta1 = ObjectDelta.createModificationAddProperty(UserType.class, USER_FOO_OID, 
    			UserType.F_FULL_NAME, PrismTestUtil.getPrismContext(), PrismTestUtil.createPolyString("baz"));
    	
    	
    	PropertyDelta<PolyString> fullNameDelta2 = PropertyDelta.createDelta(UserType.F_FULL_NAME, UserType.class, 
    			PrismTestUtil.getPrismContext());
    	PrismPropertyValue<PolyString> fullNameValue2 = new PrismPropertyValue<PolyString>(PrismTestUtil.createPolyString("baz"));
    	// Set some metadata to spoil usual equals
    	fullNameValue2.setOriginType(OriginType.OUTBOUND);
    	fullNameDelta2.addValueToAdd(fullNameValue2);
    	ObjectDelta<UserType> userDelta2 = ObjectDelta.createModifyDelta(USER_FOO_OID, fullNameDelta2, UserType.class, 
    			PrismTestUtil.getPrismContext());
				
		// WHEN
        ObjectDelta<UserType> userDeltaUnion = ObjectDelta.union(userDelta1, userDelta2);
        
        // THEN
        assertUnion01Delta(userDeltaUnion);
    }
	
	private void assertUnion01Delta(ObjectDelta<UserType> userDeltaUnion) {
		PropertyDelta<PolyString> fullNameDeltaUnion = getCheckedPropertyDeltaFromUnion(userDeltaUnion);
        Collection<PrismPropertyValue<PolyString>> valuesToAdd = fullNameDeltaUnion.getValuesToAdd();
        assertNotNull("No valuesToAdd in fullName delta after union", valuesToAdd);
        assertEquals("Unexpected size of valuesToAdd in fullName delta after union", 1, valuesToAdd.size());
        PrismPropertyValue<PolyString> valueToAdd = valuesToAdd.iterator().next();
        assertEquals("Unexcted value in valuesToAdd in fullName delta after union", 
        		PrismTestUtil.createPolyString("baz"), valueToAdd.getValue());
	}
	
	/**
	 * MODIFY/replace + MODIFY/replace
	 */
	@Test
    public void testObjectDeltaUnion02() throws Exception {
		System.out.println("\n\n===[ testObjectDeltaUnion02 ]===\n");
		// GIVEN
		
		//Delta
    	ObjectDelta<UserType> userDelta1 = ObjectDelta.createModificationReplaceProperty(UserType.class, USER_FOO_OID, 
    			UserType.F_FULL_NAME, PrismTestUtil.getPrismContext());
    	ObjectDelta<UserType> userDelta2 = ObjectDelta.createModificationReplaceProperty(UserType.class, USER_FOO_OID, 
    			UserType.F_FULL_NAME, PrismTestUtil.getPrismContext(), PrismTestUtil.createPolyString("baz"));
				
		// WHEN
        ObjectDelta<UserType> userDeltaUnion = ObjectDelta.union(userDelta1, userDelta2);
        
        // THEN
        PropertyDelta<PolyString> fullNameDeltaUnion = getCheckedPropertyDeltaFromUnion(userDeltaUnion);
        Collection<PrismPropertyValue<PolyString>> valuesToReplace = fullNameDeltaUnion.getValuesToReplace();
        assertNotNull("No valuesToReplace in fullName delta after union", valuesToReplace);
        assertEquals("Unexpected size of valuesToReplace in fullName delta after union", 1, valuesToReplace.size());
        PrismPropertyValue<PolyString> valueToReplace = valuesToReplace.iterator().next();
        assertEquals("Unexcted value in valueToReplace in fullName delta after union", 
        		PrismTestUtil.createPolyString("baz"), valueToReplace.getValue());
    }
	
	/**
	 * MODIFY/replace + MODIFY/add
	 */
	@Test
    public void testObjectDeltaUnion03() throws Exception {
		System.out.println("\n\n===[ testObjectDeltaUnion03 ]===\n");
		// GIVEN
		
		//Delta
    	ObjectDelta<UserType> userDelta1 = ObjectDelta.createModificationReplaceProperty(UserType.class, USER_FOO_OID, 
    			UserType.F_FULL_NAME, PrismTestUtil.getPrismContext());
    	ObjectDelta<UserType> userDelta2 = ObjectDelta.createModificationAddProperty(UserType.class, USER_FOO_OID, 
    			UserType.F_FULL_NAME, PrismTestUtil.getPrismContext(), PrismTestUtil.createPolyString("baz"));
				
		// WHEN
        ObjectDelta<UserType> userDeltaUnion = ObjectDelta.union(userDelta1, userDelta2);
        
        // THEN
        PropertyDelta<PolyString> fullNameDeltaUnion = getCheckedPropertyDeltaFromUnion(userDeltaUnion);
        Collection<PrismPropertyValue<PolyString>> valuesToReplace = fullNameDeltaUnion.getValuesToReplace();
        assertNotNull("No valuesToReplace in fullName delta after union", valuesToReplace);
        assertEquals("Unexpected size of valuesToReplace in fullName delta after union", 1, valuesToReplace.size());
        PrismPropertyValue<PolyString> valueToReplace = valuesToReplace.iterator().next();
        assertEquals("Unexcted value in valueToReplace in fullName delta after union", 
        		PrismTestUtil.createPolyString("baz"), valueToReplace.getValue());
    }
	
	private PropertyDelta<PolyString> getCheckedPropertyDeltaFromUnion(ObjectDelta<UserType> userDeltaUnion) {
		userDeltaUnion.checkConsistence();
        assertEquals("Wrong OID", USER_FOO_OID, userDeltaUnion.getOid());
        PrismAsserts.assertIsModify(userDeltaUnion);
        PrismAsserts.assertModifications(userDeltaUnion, 1);
        PropertyDelta<PolyString> fullNameDeltaUnion = userDeltaUnion.findPropertyDelta(UserType.F_FULL_NAME);
        assertNotNull("No fullName delta after union", fullNameDeltaUnion);
        return fullNameDeltaUnion;
	}

	@Test
    public void testObjectDeltaSummarizeModifyAdd() throws Exception {
		System.out.println("\n\n===[ testObjectDeltaSummarizeModifyAdd ]===\n");
		// GIVEN
		
    	ObjectDelta<UserType> userDelta1 = ObjectDelta.createModificationAddProperty(UserType.class, USER_FOO_OID, 
    			UserType.F_ADDITIONAL_NAMES, PrismTestUtil.getPrismContext(), PrismTestUtil.createPolyString("foo"));
    	ObjectDelta<UserType> userDelta2 = ObjectDelta.createModificationAddProperty(UserType.class, USER_FOO_OID, 
    			UserType.F_ADDITIONAL_NAMES, PrismTestUtil.getPrismContext(), PrismTestUtil.createPolyString("bar"));
				
		// WHEN
        ObjectDelta<UserType> userDeltaSum = ObjectDelta.summarize(userDelta1, userDelta2);
        
        // THEN
        assertEquals("Wrong OID", USER_FOO_OID, userDeltaSum.getOid());
        PrismAsserts.assertIsModify(userDeltaSum);
        PrismAsserts.assertModifications(userDeltaSum, 1);
        PropertyDelta<PolyString> namesDeltaUnion = userDeltaSum.findPropertyDelta(UserType.F_ADDITIONAL_NAMES);
        assertNotNull("No additionalNames delta after summarize", namesDeltaUnion);
        PrismAsserts.assertAdd(namesDeltaUnion, PrismTestUtil.createPolyString("foo"), PrismTestUtil.createPolyString("bar"));
    }
	
	@Test
    public void testObjectDeltaSummarizeModifyReplace() throws Exception {
		System.out.println("\n\n===[ testObjectDeltaSummarizeModifyReplace ]===\n");
		// GIVEN
		
    	ObjectDelta<UserType> userDelta1 = ObjectDelta.createModificationReplaceProperty(UserType.class, USER_FOO_OID, 
    			UserType.F_FULL_NAME, PrismTestUtil.getPrismContext(), PrismTestUtil.createPolyString("foo"));
    	ObjectDelta<UserType> userDelta2 = ObjectDelta.createModificationReplaceProperty(UserType.class, USER_FOO_OID, 
    			UserType.F_FULL_NAME, PrismTestUtil.getPrismContext(), PrismTestUtil.createPolyString("bar"));
				
		// WHEN
        ObjectDelta<UserType> userDeltaSum = ObjectDelta.summarize(userDelta1, userDelta2);
        
        // THEN
        assertEquals("Wrong OID", USER_FOO_OID, userDeltaSum.getOid());
        PrismAsserts.assertIsModify(userDeltaSum);
        PrismAsserts.assertModifications(userDeltaSum, 1);
        PropertyDelta<PolyString> fullNameDeltaUnion = userDeltaSum.findPropertyDelta(UserType.F_FULL_NAME);
        assertNotNull("No fullName delta after summarize", fullNameDeltaUnion);
        PrismAsserts.assertReplace(fullNameDeltaUnion, PrismTestUtil.createPolyString("bar"));
    }
	
	@Test
    public void testObjectDeltaSummarizeModifyMix() throws Exception {
		System.out.println("\n\n===[ testObjectDeltaSummarizeModifyMix ]===\n");
		// GIVEN
		
    	ObjectDelta<UserType> userDelta1 = ObjectDelta.createModificationAddProperty(UserType.class, USER_FOO_OID, 
    			UserType.F_ADDITIONAL_NAMES, PrismTestUtil.getPrismContext(), PrismTestUtil.createPolyString("baz"));
    	ObjectDelta<UserType> userDelta2 = ObjectDelta.createModificationReplaceProperty(UserType.class, USER_FOO_OID, 
    			UserType.F_ADDITIONAL_NAMES, PrismTestUtil.getPrismContext(), PrismTestUtil.createPolyString("foo"));
    	ObjectDelta<UserType> userDelta3 = ObjectDelta.createModificationAddProperty(UserType.class, USER_FOO_OID, 
    			UserType.F_ADDITIONAL_NAMES, PrismTestUtil.getPrismContext(), PrismTestUtil.createPolyString("bar"));
				
		// WHEN
        ObjectDelta<UserType> userDeltaSum = ObjectDelta.summarize(userDelta1, userDelta2, userDelta3);
        
        // THEN
        assertEquals("Wrong OID", USER_FOO_OID, userDeltaSum.getOid());
        PrismAsserts.assertIsModify(userDeltaSum);
        PrismAsserts.assertModifications(userDeltaSum, 1);
        PropertyDelta<PolyString> namesDeltaUnion = userDeltaSum.findPropertyDelta(UserType.F_ADDITIONAL_NAMES);
        assertNotNull("No additionalNames delta after summarize", namesDeltaUnion);
        PrismAsserts.assertReplace(namesDeltaUnion, PrismTestUtil.createPolyString("foo"), PrismTestUtil.createPolyString("bar"));
    }
	
	@Test
    public void testObjectDeltaSummarizeAddModifyMix() throws Exception {
		System.out.println("\n\n===[ testObjectDeltaSummarizeAddModifyMix ]===\n");
		// GIVEN
		
		PrismObject<UserType> user = createUser();
		ObjectDelta<UserType> userDelta0 = ObjectDelta.createAddDelta(user);
    	ObjectDelta<UserType> userDelta1 = ObjectDelta.createModificationAddProperty(UserType.class, USER_FOO_OID, 
    			UserType.F_ADDITIONAL_NAMES, PrismTestUtil.getPrismContext(), PrismTestUtil.createPolyString("baz"));
    	ObjectDelta<UserType> userDelta2 = ObjectDelta.createModificationReplaceProperty(UserType.class, USER_FOO_OID, 
    			UserType.F_ADDITIONAL_NAMES, PrismTestUtil.getPrismContext(), PrismTestUtil.createPolyString("foo"));
    	ObjectDelta<UserType> userDelta3 = ObjectDelta.createModificationAddProperty(UserType.class, USER_FOO_OID, 
    			UserType.F_ADDITIONAL_NAMES, PrismTestUtil.getPrismContext(), PrismTestUtil.createPolyString("bar"));
				
		// WHEN
        ObjectDelta<UserType> userDeltaSum = ObjectDelta.summarize(userDelta0, userDelta1, userDelta2, userDelta3);
        
        // THEN
        assertEquals("Wrong OID", USER_FOO_OID, userDeltaSum.getOid());
        PrismAsserts.assertIsAdd(userDeltaSum);
        PrismObject<UserType> userSum = userDeltaSum.getObjectToAdd();
        assert user != userSum : "User was not clonned";
        PrismAsserts.assertPropertyValue(userSum, UserType.F_ADDITIONAL_NAMES, 
        		PrismTestUtil.createPolyString("foo"), PrismTestUtil.createPolyString("bar"));
        // TODO
    }
	
	@Test
    public void testDeltaComplex() throws Exception {
		System.out.println("\n\n===[ testDeltaComplex ]===\n");
		// GIVEN
		
    	ObjectDelta<UserType> delta = ObjectDelta.createModificationAddProperty(UserType.class, USER_FOO_OID, 
    			UserType.F_FULL_NAME, PrismTestUtil.getPrismContext(), PrismTestUtil.createPolyString("Foo Bar"));
    	
    	PrismObjectDefinition<UserType> userTypeDefinition = getUserTypeDefinition();
    	
    	PrismContainerDefinition<ActivationType> activationDefinition = userTypeDefinition.findContainerDefinition(UserType.F_ACTIVATION);
    	PrismContainer<ActivationType> activationContainer = activationDefinition.instantiate();
    	PrismPropertyDefinition enabledDef = activationDefinition.findPropertyDefinition(ActivationType.F_ENABLED);
    	PrismProperty<Boolean> enabledProperty = enabledDef.instantiate();
    	enabledProperty.setRealValue(true);
    	activationContainer.add(enabledProperty);
    	delta.addModificationDeleteContainer(UserType.F_ACTIVATION, activationContainer.getValue().clone());
		
    	PrismContainerDefinition<AssignmentType> assDef = userTypeDefinition.findContainerDefinition(UserType.F_ASSIGNMENT);
    	PrismPropertyDefinition descDef = assDef.findPropertyDefinition(AssignmentType.F_DESCRIPTION);
    	
    	PrismContainerValue<AssignmentType> assVal1 = new PrismContainerValue<AssignmentType>();
    	assVal1.setId(111L);
    	PrismProperty<String> descProp1 = descDef.instantiate();
    	descProp1.setRealValue("desc 1");
    	assVal1.add(descProp1);

    	PrismContainerValue<AssignmentType> assVal2 = new PrismContainerValue<AssignmentType>();
    	assVal2.setId(222L);
    	PrismProperty<String> descProp2 = descDef.instantiate();
    	descProp2.setRealValue("desc 2");
    	assVal2.add(descProp2);
    	
    	delta.addModificationAddContainer(UserType.F_ASSIGNMENT, assVal1, assVal2);
    	
		System.out.println("Delta:");
		System.out.println(delta.dump());
				
		// WHEN, THEN
		PrismInternalTestUtil.assertVisitor(delta, 14);
		
		PrismInternalTestUtil.assertPathVisitor(delta, new ItemPath(UserType.F_FULL_NAME), true, 2);
		PrismInternalTestUtil.assertPathVisitor(delta, new ItemPath(UserType.F_ACTIVATION), true, 4);
		PrismInternalTestUtil.assertPathVisitor(delta, new ItemPath(UserType.F_ACTIVATION, ActivationType.F_ENABLED), true, 2);
		PrismInternalTestUtil.assertPathVisitor(delta, new ItemPath(UserType.F_ASSIGNMENT), true, 7);
		PrismInternalTestUtil.assertPathVisitor(delta, new ItemPath(
				new NameItemPathSegment(UserType.F_ASSIGNMENT),
				IdItemPathSegment.WILDCARD), true, 6);
		
		PrismInternalTestUtil.assertPathVisitor(delta, new ItemPath(UserType.F_FULL_NAME), false, 1);
		PrismInternalTestUtil.assertPathVisitor(delta, new ItemPath(UserType.F_ACTIVATION), false, 1);
		PrismInternalTestUtil.assertPathVisitor(delta, new ItemPath(UserType.F_ACTIVATION, ActivationType.F_ENABLED), false, 1);
		PrismInternalTestUtil.assertPathVisitor(delta, new ItemPath(UserType.F_ASSIGNMENT), false, 1);
		PrismInternalTestUtil.assertPathVisitor(delta, new ItemPath(
				new NameItemPathSegment(UserType.F_ASSIGNMENT),
				IdItemPathSegment.WILDCARD), false, 2);
    }
	

	private PrismObject<UserType> createUser() throws SchemaException {
		PrismObjectDefinition<UserType> userDef = getUserTypeDefinition();

		PrismObject<UserType> user = userDef.instantiate();
		user.setOid(USER_FOO_OID);
		user.setPropertyRealValue(UserType.F_NAME, PrismTestUtil.createPolyString("foo"));
		PrismProperty<PolyString> anamesProp = user.findOrCreateProperty(UserType.F_ADDITIONAL_NAMES);
		anamesProp.addRealValue(PrismTestUtil.createPolyString("foobar"));
		
		PrismContainer<AssignmentType> assignment = user.findOrCreateContainer(UserType.F_ASSIGNMENT);
    	PrismContainerValue<AssignmentType> assignmentValue = assignment.createNewValue();
    	assignmentValue.setId(123L);
    	assignmentValue.setPropertyRealValue(AssignmentType.F_DESCRIPTION, "chamalalia patlama paprtala");
    	
    	return user;
	}


}
