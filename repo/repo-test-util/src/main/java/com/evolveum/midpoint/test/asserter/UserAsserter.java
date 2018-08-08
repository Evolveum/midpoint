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
package com.evolveum.midpoint.test.asserter;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 *
 */
public class UserAsserter<RA> extends FocusAsserter<UserType,RA> {
	
	public UserAsserter(PrismObject<UserType> focus) {
		super(focus);
	}
	
	public UserAsserter(PrismObject<UserType> focus, String details) {
		super(focus, details);
	}
	
	public UserAsserter(PrismObject<UserType> focus, RA returnAsserter, String details) {
		super(focus, returnAsserter, details);
	}
	
	public static UserAsserter<Void> forUser(PrismObject<UserType> focus) {
		return new UserAsserter<>(focus);
	}
	
	public static UserAsserter<Void> forUser(PrismObject<UserType> focus, String details) {
		return new UserAsserter<>(focus, details);
	}
	
	@Override
	public UserAsserter<RA> assertOid() {
		super.assertOid();
		return this;
	}
	
	@Override
	public UserAsserter<RA> assertOid(String expected) {
		super.assertOid(expected);
		return this;
	}
	
	@Override
	public UserAsserter<RA> assertOidDifferentThan(String oid) {
		super.assertOidDifferentThan(oid);
		return this;
	}
	
	@Override
	public UserAsserter<RA> assertName() {
		super.assertName();
		return this;
	}
	
	@Override
	public UserAsserter<RA> assertName(String expectedOrig) {
		super.assertName(expectedOrig);
		return this;
	}
	
	@Override
	public UserAsserter<RA> assertLifecycleState(String expected) {
		super.assertLifecycleState(expected);
		return this;
	}
	
	@Override
	public UserAsserter<RA> assertActiveLifecycleState() {
		super.assertActiveLifecycleState();
		return this;
	}
	
	public UserAsserter<RA> assertAdministrativeStatus(ActivationStatusType expected) {
		ActivationType activation = getActivation();
		if (activation == null) {
			if (expected == null) {
				return this;
			} else {
				fail("No activation in "+desc());
			}
		}
		assertEquals("Wrong activation administrativeStatus in "+desc(), expected, activation.getAdministrativeStatus());
		return this;
	}
	
	private ActivationType getActivation() {
		return getObject().asObjectable().getActivation();
	}
	
	public UserAsserter<RA> display() {
		super.display();
		return this;
	}
	
	public UserAsserter<RA> display(String message) {
		super.display(message);
		return this;
	}
	
	@Override
	public LinksAsserter<UserType, UserAsserter<RA>, RA> links() {
		LinksAsserter<UserType, UserAsserter<RA>, RA> asserter = new LinksAsserter<>(this, getDetails());
		copySetupTo(asserter);
		return asserter;
	}
	
	@Override
	public UserAsserter<RA> assertLinks(int expected) {
		super.assertLinks(expected);
		return this;
	}
	
	@Override
	public AssignmentsAsserter<UserType, UserAsserter<RA>, RA> assignments() {
		AssignmentsAsserter<UserType, UserAsserter<RA>, RA> asserter = new AssignmentsAsserter<>(this, getDetails());
		copySetupTo(asserter);
		return asserter;
	}

	public UserAsserter<RA> assertFullName(String expectedOrig) {
		assertPolyStringProperty(UserType.F_FULL_NAME, expectedOrig);
		return this;
	}
	
	public UserAsserter<RA> assertLocality(String expectedOrig) {
		assertPolyStringProperty(UserType.F_LOCALITY, expectedOrig);
		return this;
	}
	
	@Override
	public UserAsserter<RA> assertHasProjectionOnResource(String resourceOid) throws ObjectNotFoundException, SchemaException {
		super.assertHasProjectionOnResource(resourceOid);
		return this;
	}
	
	@Override
	public ShadowAsserter<UserAsserter<RA>> projectionOnResource(String resourceOid) throws ObjectNotFoundException, SchemaException {
		return super.projectionOnResource(resourceOid);
	}
	
	@Override
	public UserAsserter<RA> displayWithProjections() throws ObjectNotFoundException, SchemaException {
		super.displayWithProjections();
		return this;
	}

	@Override
	public ShadowReferenceAsserter<UserAsserter<RA>> singleLink() {
		return (ShadowReferenceAsserter<UserAsserter<RA>>) super.singleLink();
	}

	@Override
	public UserAsserter<RA> assertAssignments(int expected) {
		super.assertAssignments(expected);
		return this;
	}
	
	
}
