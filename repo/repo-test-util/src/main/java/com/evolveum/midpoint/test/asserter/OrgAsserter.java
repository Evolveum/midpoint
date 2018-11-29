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
import com.evolveum.midpoint.prism.path.UniformItemPath;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;

/**
 * @author semancik
 *
 */
public class OrgAsserter<RA> extends AbstractRoleAsserter<OrgType,RA> {
	
	public OrgAsserter(PrismObject<OrgType> focus) {
		super(focus);
	}
	
	public OrgAsserter(PrismObject<OrgType> focus, String details) {
		super(focus, details);
	}
	
	public OrgAsserter(PrismObject<OrgType> focus, RA returnAsserter, String details) {
		super(focus, returnAsserter, details);
	}
	
	public static OrgAsserter<Void> forOrg(PrismObject<OrgType> focus) {
		return new OrgAsserter<>(focus);
	}
	
	public static OrgAsserter<Void> forOrg(PrismObject<OrgType> focus, String details) {
		return new OrgAsserter<>(focus, details);
	}
	
	// It is insane to override all those methods from superclass.
	// But there is no better way to specify something like <SELF> type in Java.
	// This is lesser evil.
	@Override
	public OrgAsserter<RA> assertOid() {
		super.assertOid();
		return this;
	}
	
	@Override
	public OrgAsserter<RA> assertOid(String expected) {
		super.assertOid(expected);
		return this;
	}
	
	@Override
	public OrgAsserter<RA> assertOidDifferentThan(String oid) {
		super.assertOidDifferentThan(oid);
		return this;
	}
	
	@Override
	public OrgAsserter<RA> assertName() {
		super.assertName();
		return this;
	}
	
	@Override
	public OrgAsserter<RA> assertName(String expectedOrig) {
		super.assertName(expectedOrig);
		return this;
	}
	
	@Override
	public OrgAsserter<RA> assertDescription(String expected) {
		super.assertDescription(expected);
		return this;
	}
	
	@Override
	public OrgAsserter<RA> assertSubtype(String... expected) {
		super.assertSubtype(expected);
		return this;
	}
	
	@Override
	public OrgAsserter<RA> assertTenantRef(String expectedOid) {
		super.assertTenantRef(expectedOid);
		return this;
	}
	
	@Override
	public OrgAsserter<RA> assertLifecycleState(String expected) {
		super.assertLifecycleState(expected);
		return this;
	}
	
	@Override
	public OrgAsserter<RA> assertActiveLifecycleState() {
		super.assertActiveLifecycleState();
		return this;
	}
	
	@Override
	public OrgAsserter<RA> assertAdministrativeStatus(ActivationStatusType expected) {
		super.assertAdministrativeStatus(expected);
		return this;
	}
		
	@Override
	public OrgAsserter<RA> display() {
		super.display();
		return this;
	}
	
	@Override
	public OrgAsserter<RA> display(String message) {
		super.display(message);
		return this;
	}
	
	@Override
	public ActivationAsserter<OrgType, OrgAsserter<RA>, RA> activation() {
		ActivationAsserter<OrgType, OrgAsserter<RA>, RA> asserter = new ActivationAsserter<>(this, getDetails());
		copySetupTo(asserter);
		return asserter;
	}

	@Override
	public LinksAsserter<OrgType, OrgAsserter<RA>, RA> links() {
		LinksAsserter<OrgType, OrgAsserter<RA>, RA> asserter = new LinksAsserter<>(this, getDetails());
		copySetupTo(asserter);
		return asserter;
	}
	
	@Override
	public OrgAsserter<RA> assertLinks(int expected) {
		super.assertLinks(expected);
		return this;
	}
	
	@Override
	public AssignmentsAsserter<OrgType, OrgAsserter<RA>, RA> assignments() {
		AssignmentsAsserter<OrgType, OrgAsserter<RA>, RA> asserter = new AssignmentsAsserter<>(this, getDetails());
		copySetupTo(asserter);
		return asserter;
	}

	@Override
	public OrgAsserter<RA> assertDisplayName(String expectedOrig) {
		super.assertDisplayName(expectedOrig);
		return this;
	}
	
	@Override
	public OrgAsserter<RA> assertLocality(String expectedOrig) {
		super.assertLocality(expectedOrig);
		return this;
	}
	
	@Override
	public OrgAsserter<RA> assertIdentifier(String expectedOrig) {
		super.assertIdentifier(expectedOrig);
		return this;
	}
	
	public OrgAsserter<RA> assertTenant(Boolean expected) {
		assertPropertyEquals(OrgType.F_TENANT, expected);
		return this;
	}
	
	public OrgAsserter<RA> assertIsTenant() {
		assertPropertyEquals(OrgType.F_TENANT, true);
		return this;
	}
	
	@Override
	public OrgAsserter<RA> assertHasProjectionOnResource(String resourceOid) throws ObjectNotFoundException, SchemaException {
		super.assertHasProjectionOnResource(resourceOid);
		return this;
	}
	
	@Override
	public ShadowAsserter<OrgAsserter<RA>> projectionOnResource(String resourceOid) throws ObjectNotFoundException, SchemaException {
		return (ShadowAsserter<OrgAsserter<RA>>) super.projectionOnResource(resourceOid);
	}
	
	@Override
	public OrgAsserter<RA> displayWithProjections() throws ObjectNotFoundException, SchemaException {
		super.displayWithProjections();
		return this;
	}

	@Override
	public ShadowReferenceAsserter<OrgAsserter<RA>> singleLink() {
		return (ShadowReferenceAsserter<OrgAsserter<RA>>) super.singleLink();
	}

	@Override
	public OrgAsserter<RA> assertAssignments(int expected) {
		super.assertAssignments(expected);
		return this;
	}
	
	@Override
	public ParentOrgRefsAsserter<OrgType, OrgAsserter<RA>, RA> parentOrgRefs() {
		ParentOrgRefsAsserter<OrgType, OrgAsserter<RA>, RA> asserter = new ParentOrgRefsAsserter<>(this, getDetails());
		copySetupTo(asserter);
		return asserter;
	}
	
	@Override
	public OrgAsserter<RA> assertParentOrgRefs(String... expectedOids) {
		super.assertParentOrgRefs(expectedOids);
		return this;
	}
	
	@Override
	public RoleMembershipRefsAsserter<OrgType, ? extends OrgAsserter<RA>, RA> roleMembershipRefs() {
		RoleMembershipRefsAsserter<OrgType,OrgAsserter<RA>,RA> asserter = new RoleMembershipRefsAsserter<>(this, getDetails());
		copySetupTo(asserter);
		return asserter;
	}
	
	@Override
	public OrgAsserter<RA> assertRoleMemberhipRefs(int expected) {
		super.assertRoleMemberhipRefs(expected);
		return this;
	}
	
	@Override
	public ExtensionAsserter<OrgType, ? extends OrgAsserter<RA>, RA> extension() {
		ExtensionAsserter<OrgType, ? extends OrgAsserter<RA>, RA> asserter = new ExtensionAsserter<>(this, getDetails());
		copySetupTo(asserter);
		return asserter;
	}
	
	@Override
	public TriggersAsserter<OrgType, ? extends OrgAsserter<RA>, RA> triggers() {
		TriggersAsserter<OrgType, ? extends OrgAsserter<RA>, RA> asserter = new TriggersAsserter<>(this, getDetails());
		copySetupTo(asserter);
		return asserter;
	}
	
	@Override
	public OrgAsserter<RA> assertNoItem(QName itemName) {
		super.assertNoItem(itemName);
		return this;
	}
	
	@Override
	public OrgAsserter<RA> assertNoItem(UniformItemPath itemPath) {
		super.assertNoItem(itemPath);
		return this;
	}
}
