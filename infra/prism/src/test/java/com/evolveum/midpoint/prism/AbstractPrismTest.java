/**
 * Copyright (c) 2018 Evolveum
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
package com.evolveum.midpoint.prism;

import static com.evolveum.midpoint.prism.PrismInternalTestUtil.DEFAULT_NAMESPACE_PREFIX;
import static org.testng.AssertJUnit.assertEquals;

import java.io.IOException;
import java.util.Collection;
import java.util.function.Function;

import org.testng.AssertJUnit;
import org.testng.annotations.BeforeSuite;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.foo.AssignmentType;
import com.evolveum.midpoint.prism.foo.UserType;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author semancik
 *
 */
public class AbstractPrismTest {
	
	protected static final String USER_FOO_OID = "01234567";
	
	protected static final String ASSIGNMENT_PATLAMA_DESCRIPTION = "jamalalicha patlama paprtala";
	protected static final long ASSIGNMENT_PATLAMA_ID = 123L;
	
	protected static final String ASSIGNMENT_ABRAKADABRA_DESCRIPTION = "abra kadabra";
	protected static final long ASSIGNMENT_ABRAKADABRA_ID = 222L;

	
	@BeforeSuite
	public void setupDebug() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(DEFAULT_NAMESPACE_PREFIX);
		PrismTestUtil.resetPrismContext(new PrismInternalTestUtil());
	}
	
	protected void displayTestTitle(String testName) {
		PrismTestUtil.displayTestTitle(testName);
	}
	
	protected void displayWhen(String testName) {
		PrismTestUtil.displayWhen(testName);
	}
	
	protected void displayThen(String testName) {
		PrismTestUtil.displayThen(testName);
	}

	protected void display(String title, String value) {
		PrismTestUtil.display(title, value);
	}

	protected void display(String title, DebugDumpable value) {
		PrismTestUtil.display(title, value);
	}
	
	protected void display(String title, Object value) {
		PrismTestUtil.display(title, value);
	}
	
	protected PrismObjectDefinition<UserType> getUserTypeDefinition() {
		return PrismInternalTestUtil.getUserTypeDefinition();
	}
	
	protected PolyString createPolyString(String orig) {
		return PrismTestUtil.createPolyString(orig);
	}
	
	protected PrismContext getPrismContext() {
		return PrismTestUtil.getPrismContext();
	}
	
	protected void fail(String message) {
		AssertJUnit.fail(message);
	}
	
	protected void assertAssignmentDelete(ObjectDelta<UserType> userDelta, int expectedValues) {
		assertAssignmentValueCount(userDelta, expectedValues, "delete", ItemDelta::getValuesToDelete);
	}
	
	protected void assertAssignmentAdd(ObjectDelta<UserType> userDelta, int expectedValues) {
		assertAssignmentValueCount(userDelta, expectedValues, "add", ItemDelta::getValuesToAdd);
	}
	
	protected void assertAssignmentReplace(ObjectDelta<UserType> userDelta, int expectedValues) {
		assertAssignmentValueCount(userDelta, expectedValues, "replace", ItemDelta::getValuesToReplace);
	}
	
	private void assertAssignmentValueCount(ObjectDelta<UserType> userDelta, int expectedValues, String type, Function<ContainerDelta<AssignmentType>, Collection<PrismContainerValue<AssignmentType>>> lambda) {
		ContainerDelta<AssignmentType> assignmentDelta = userDelta.findContainerDelta(UserType.F_ASSIGNMENT);
		if (assignmentDelta == null) {
			if (expectedValues == 0) {
				return;
			} else {
				fail("No assignment delta in "+userDelta);
			}
		}
		Collection<PrismContainerValue<AssignmentType>> values = lambda.apply(assignmentDelta);
		if (values == null) {
			if (expectedValues == 0) {
				return;
			} else {
				fail("No values to delete in assignment "+type+" in "+userDelta);
			}
		}
		assertEquals("Wrong number of assignment "+type+" values in "+userDelta, expectedValues, values.size());
	}
	
	protected void assertAssignmentDelete(ObjectDelta<UserType> userDelta, Long expectedId, String expectedDescription) {
		assertAssignmentValue(userDelta, expectedId, expectedDescription, "delete", ItemDelta::getValuesToDelete);
	}
	
	protected void assertAssignmentAdd(ObjectDelta<UserType> userDelta, Long expectedId, String expectedDescription) {
		assertAssignmentValue(userDelta, expectedId, expectedDescription, "add", ItemDelta::getValuesToAdd);
	}
	
	protected void assertAssignmentReplace(ObjectDelta<UserType> userDelta, Long expectedId, String expectedDescription) {
		assertAssignmentValue(userDelta, expectedId, expectedDescription, "replace", ItemDelta::getValuesToReplace);
	}
	
	private void assertAssignmentValue(ObjectDelta<UserType> userDelta, Long expectedId, String expectedDescription, String type, Function<ContainerDelta<AssignmentType>, Collection<PrismContainerValue<AssignmentType>>> lambda) {
		ContainerDelta<AssignmentType> assignmentDelta = userDelta.findContainerDelta(UserType.F_ASSIGNMENT);
		if (assignmentDelta == null) {
			fail("No assignment delta in "+userDelta);
		}
		Collection<PrismContainerValue<AssignmentType>> valuesToDelete = lambda.apply(assignmentDelta);
		if (valuesToDelete == null) {
			fail("No values to "+type+" in assignment delta in "+userDelta);
		}
		for (PrismContainerValue<AssignmentType> valueToDelete : valuesToDelete) {
			if (assignmentMatches(valueToDelete, expectedId, expectedDescription)) {
				return;
			}
		}
		fail("Assignment "+expectedId+":"+expectedDescription+" not found in "+userDelta);
	}

	protected boolean assignmentMatches(PrismContainerValue<AssignmentType> assignmentValue, Long expectedId, String expectedDescription) {
		if (assignmentValue.getId() != expectedId) {
			return false;
		}
		String description = assignmentValue.getPropertyRealValue(AssignmentType.F_DESCRIPTION, String.class);
		if (!MiscUtil.equals(expectedDescription, description)) {
			return false;
		}
		return true;
	}

	protected PrismObject<UserType> createUserFooPatlama() throws SchemaException {
		PrismObject<UserType> user = createUserFoo();
		addAssignment(user, ASSIGNMENT_PATLAMA_ID, ASSIGNMENT_PATLAMA_DESCRIPTION);
    	return user;
	}
	
	protected void addAssignment(PrismObject<UserType> user, Long id, String description) throws SchemaException {
		PrismContainer<AssignmentType> assignment = user.findOrCreateContainer(UserType.F_ASSIGNMENT);
    	PrismContainerValue<AssignmentType> assignmentValue = assignment.createNewValue();
    	populateAssignmentValue(assignmentValue, id, description);
	}
	
	protected void populateAssignmentValue(PrismContainerValue<AssignmentType> assignmentValue, Long id, String description) throws SchemaException {
    	if (id != null) {
    		assignmentValue.setId(id);
    	}
    	if (description != null) {
    		assignmentValue.setPropertyRealValue(AssignmentType.F_DESCRIPTION, description, getPrismContext());
    	}
	}
	
	protected PrismContainerValue<AssignmentType> createAssignmentValue(Long id, String description) throws SchemaException {
		PrismContainerDefinition<AssignmentType> assignmentDef = getUserTypeDefinition().findContainerDefinition(UserType.F_ASSIGNMENT);
		PrismContainerValue<AssignmentType> assignmentValue = new PrismContainerValue<>(getPrismContext());
		populateAssignmentValue(assignmentValue, id, description);
		return assignmentValue;
	}

	protected PrismObject<UserType> createUserFoo() throws SchemaException {
		PrismObjectDefinition<UserType> userDef = getUserTypeDefinition();

		PrismObject<UserType> user = userDef.instantiate();
		user.setOid(USER_FOO_OID);
		user.setPropertyRealValue(UserType.F_NAME, createPolyString("foo"));
		PrismProperty<PolyString> anamesProp = user.findOrCreateProperty(UserType.F_ADDITIONAL_NAMES);
		anamesProp.addRealValue(createPolyString("foobar"));
		
    	return user;
	}


}
