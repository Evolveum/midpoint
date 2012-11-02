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
