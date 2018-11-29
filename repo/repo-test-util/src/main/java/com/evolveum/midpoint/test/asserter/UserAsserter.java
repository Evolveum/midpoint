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

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.path.UniformItemPath;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsStorageTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

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
	
	// It is insane to override all those methods from superclass.
	// But there is no better way to specify something like <SELF> type in Java.
	// This is lesser evil.
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
	public UserAsserter<RA> assertDescription(String expected) {
		super.assertDescription(expected);
		return this;
	}
	
	@Override
	public UserAsserter<RA> assertSubtype(String... expected) {
		super.assertSubtype(expected);
		return this;
	}
	
	@Override
	public UserAsserter<RA> assertTenantRef(String expectedOid) {
		super.assertTenantRef(expectedOid);
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
	
	public UserAsserter<RA> assertPassword(String expectedClearPassword) throws SchemaException, EncryptionException {
		assertPassword(expectedClearPassword, CredentialsStorageTypeType.ENCRYPTION);
		return this;
	}
	
	public UserAsserter<RA> assertPassword(String expectedClearPassword, CredentialsStorageTypeType storageType) throws SchemaException, EncryptionException {
		CredentialsType creds = getObject().asObjectable().getCredentials();
		assertNotNull("No credentials in "+desc(), creds);
		PasswordType password = creds.getPassword();
		assertNotNull("No password in "+desc(), password);
		ProtectedStringType protectedActualPassword = password.getValue();
		IntegrationTestTools.assertProtectedString("Password for "+desc(), expectedClearPassword, protectedActualPassword, storageType, getProtector());
		return this;
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
	public ActivationAsserter<UserType, UserAsserter<RA>, RA> activation() {
		ActivationAsserter<UserType, UserAsserter<RA>, RA> asserter = new ActivationAsserter<>(this, getDetails());
		copySetupTo(asserter);
		return asserter;
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
	
	public UserAsserter<RA> assertNoFullName() {
		assertNoItem(UserType.F_FULL_NAME);
		return this;
	}
	
	public UserAsserter<RA> assertGivenName(String expectedOrig) {
		assertPolyStringProperty(UserType.F_GIVEN_NAME, expectedOrig);
		return this;
	}
	
	public UserAsserter<RA> assertNoGivenName() {
		assertNoItem(UserType.F_GIVEN_NAME);
		return this;
	}
	
	public UserAsserter<RA> assertFamilyName(String expectedOrig) {
		assertPolyStringProperty(UserType.F_FAMILY_NAME, expectedOrig);
		return this;
	}
	
	public UserAsserter<RA> assertNoFamilyName() {
		assertNoItem(UserType.F_FAMILY_NAME);
		return this;
	}
	
	public UserAsserter<RA> assertLocality(String expectedOrig) {
		assertPolyStringProperty(UserType.F_LOCALITY, expectedOrig);
		return this;
	}
	
	public UserAsserter<RA> assertOrganizationalUnit(String expectedOrig) {
		assertPolyStringProperty(UserType.F_ORGANIZATIONAL_UNIT, expectedOrig);
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
	
	@Override
	public ParentOrgRefsAsserter<UserType, UserAsserter<RA>, RA> parentOrgRefs() {
		ParentOrgRefsAsserter<UserType, UserAsserter<RA>, RA> asserter = new ParentOrgRefsAsserter<>(this, getDetails());
		copySetupTo(asserter);
		return asserter;
	}
	
	@Override
	public UserAsserter<RA> assertParentOrgRefs(String... expectedOids) {
		super.assertParentOrgRefs(expectedOids);
		return this;
	}
	
	@Override
	public RoleMembershipRefsAsserter<UserType, ? extends UserAsserter<RA>, RA> roleMembershipRefs() {
		RoleMembershipRefsAsserter<UserType,UserAsserter<RA>,RA> asserter = new RoleMembershipRefsAsserter<>(this, getDetails());
		copySetupTo(asserter);
		return asserter;
	}
	
	@Override
	public UserAsserter<RA> assertRoleMemberhipRefs(int expected) {
		super.assertRoleMemberhipRefs(expected);
		return this;
	}
	
	@Override
	public ExtensionAsserter<UserType, ? extends UserAsserter<RA>, RA> extension() {
		ExtensionAsserter<UserType, ? extends UserAsserter<RA>, RA> asserter = new ExtensionAsserter<>(this, getDetails());
		copySetupTo(asserter);
		return asserter;
	}
	
	@Override
	public TriggersAsserter<UserType, ? extends UserAsserter<RA>, RA> triggers() {
		TriggersAsserter<UserType, ? extends UserAsserter<RA>, RA> asserter = new TriggersAsserter<>(this, getDetails());
		copySetupTo(asserter);
		return asserter;
	}
	
	@Override
	public UserAsserter<RA> assertNoItem(QName itemName) {
		super.assertNoItem(itemName);
		return this;
	}
	
	@Override
	public UserAsserter<RA> assertNoItem(UniformItemPath itemPath) {
		super.assertNoItem(itemPath);
		return this;
	}
}
