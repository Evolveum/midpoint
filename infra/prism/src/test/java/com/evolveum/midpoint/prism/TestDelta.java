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

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.foo.AssignmentType;
import com.evolveum.midpoint.prism.foo.UserType;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
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
	
	@Test
    public void testObjectDeltaUnionSimple() throws Exception {
		System.out.println("\n\n===[ testObjectDeltaUnionSimple ]===\n");
		// GIVEN
		
		//Delta
    	ObjectDelta<UserType> userDelta1 = ObjectDelta.createModificationAddProperty(UserType.class, USER_FOO_OID, 
    			UserType.F_FULL_NAME, PrismTestUtil.getPrismContext(), PrismTestUtil.createPolyString("baz"));
    	ObjectDelta<UserType> userDelta2 = ObjectDelta.createModificationAddProperty(UserType.class, USER_FOO_OID, 
    			UserType.F_FULL_NAME, PrismTestUtil.getPrismContext(), PrismTestUtil.createPolyString("baz"));
				
		// WHEN
        ObjectDelta<UserType> userDeltaUnion = ObjectDelta.union(userDelta1, userDelta2);
        
        // THEN
        assertUnionDelta(userDeltaUnion);
    }
	
	@Test
    public void testObjectDeltaUnionMetadata() throws Exception {
		System.out.println("\n\n===[ testObjectDeltaUnionMetadata ]===\n");
		// GIVEN
		
		//Delta
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
        assertUnionDelta(userDeltaUnion);
    }
	
	private void assertUnionDelta(ObjectDelta<UserType> userDeltaUnion) {
        assertEquals("Wrong OID", USER_FOO_OID, userDeltaUnion.getOid());
        PrismAsserts.assertIsModify(userDeltaUnion);
        PrismAsserts.assertModifications(userDeltaUnion, 1);
        PropertyDelta<PolyString> fullNameDeltaUnion = userDeltaUnion.findPropertyDelta(UserType.F_FULL_NAME);
        assertNotNull("No fullName delta after union", fullNameDeltaUnion);
        Collection<PrismPropertyValue<PolyString>> valuesToAdd = fullNameDeltaUnion.getValuesToAdd();
        assertNotNull("No valuesToAdd in fullName delta after union", valuesToAdd);
        assertEquals("Unexpected size of valuesToAdd in fullName delta after union", 1, valuesToAdd.size());
        PrismPropertyValue<PolyString> valueToAdd = valuesToAdd.iterator().next();
        assertEquals("Unexcted value in valuesToAdd in fullName delta after union", 
        		PrismTestUtil.createPolyString("baz"), valueToAdd.getValue());
	}

	public PrismObject<UserType> createUser() throws SchemaException {
		PrismObjectDefinition<UserType> userDef = getUserTypeDefinition();

		PrismObject<UserType> user = userDef.instantiate();
		user.setOid(USER_FOO_OID);
		user.setPropertyRealValue(UserType.F_NAME, PrismTestUtil.createPolyString("foo"));
		PrismProperty<PolyString> anamesProp = user.findOrCreateProperty(UserType.F_ADDITIONAL_NAMES);
		anamesProp.addRealValue(PrismTestUtil.createPolyString("foobar"));
		
		PrismContainer<AssignmentType> assignment = user.findOrCreateContainer(UserType.F_ASSIGNMENT);
    	PrismContainerValue<AssignmentType> assignmentValue = assignment.createNewValue();
    	assignmentValue.setId("123");
    	assignmentValue.setPropertyRealValue(AssignmentType.F_DESCRIPTION, "chamalalia patlama paprtala");
    	
    	return user;
	}


}
