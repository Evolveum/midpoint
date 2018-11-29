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
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;

/**
 * @author semancik
 *
 */
public class AbstractRoleAsserter<F extends AbstractRoleType, RA> extends FocusAsserter<F,RA> {
	
	public AbstractRoleAsserter(PrismObject<F> focus) {
		super(focus);
	}
	
	public AbstractRoleAsserter(PrismObject<F> focus, String details) {
		super(focus, details);
	}
	
	public AbstractRoleAsserter(PrismObject<F> focus, RA returnAsserter, String details) {
		super(focus, returnAsserter, details);
	}
	
	public static <F extends AbstractRoleType> AbstractRoleAsserter<F,Void> forAbstractRole(PrismObject<F> focus) {
		return new AbstractRoleAsserter<>(focus);
	}
	
	public static <F extends AbstractRoleType> AbstractRoleAsserter<F,Void> forAbstractRole(PrismObject<F> focus, String details) {
		return new AbstractRoleAsserter<>(focus, details);
	}
	
	// It is insane to override all those methods from superclass.
	// But there is no better way to specify something like <SELF> type in Java.
	// This is lesser evil.
	@Override
	public AbstractRoleAsserter<F,RA> assertOid() {
		super.assertOid();
		return this;
	}
	
	@Override
	public AbstractRoleAsserter<F,RA> assertOid(String expected) {
		super.assertOid(expected);
		return this;
	}
	
	@Override
	public AbstractRoleAsserter<F,RA> assertOidDifferentThan(String oid) {
		super.assertOidDifferentThan(oid);
		return this;
	}
	
	@Override
	public AbstractRoleAsserter<F,RA> assertName() {
		super.assertName();
		return this;
	}
	
	@Override
	public AbstractRoleAsserter<F,RA> assertName(String expectedOrig) {
		super.assertName(expectedOrig);
		return this;
	}
	
	@Override
	public AbstractRoleAsserter<F,RA> assertDescription(String expected) {
		super.assertDescription(expected);
		return this;
	}
	
	@Override
	public AbstractRoleAsserter<F,RA> assertSubtype(String... expected) {
		super.assertSubtype(expected);
		return this;
	}
	
	@Override
	public AbstractRoleAsserter<F,RA> assertTenantRef(String expectedOid) {
		super.assertTenantRef(expectedOid);
		return this;
	}
	
	@Override
	public AbstractRoleAsserter<F,RA> assertLifecycleState(String expected) {
		super.assertLifecycleState(expected);
		return this;
	}
	
	@Override
	public AbstractRoleAsserter<F,RA> assertActiveLifecycleState() {
		super.assertActiveLifecycleState();
		return this;
	}
	
	@Override
	public AbstractRoleAsserter<F,RA> assertAdministrativeStatus(ActivationStatusType expected) {
		super.assertAdministrativeStatus(expected);
		return this;
	}
	
	@Override
	public AbstractRoleAsserter<F,RA> display() {
		super.display();
		return this;
	}
	
	@Override
	public AbstractRoleAsserter<F,RA> display(String message) {
		super.display(message);
		return this;
	}
	
	@Override
	public ActivationAsserter<F, ? extends AbstractRoleAsserter<F,RA>, RA> activation() {
		ActivationAsserter<F, AbstractRoleAsserter<F,RA>, RA> asserter = new ActivationAsserter<>(this, getDetails());
		copySetupTo(asserter);
		return asserter;
	}

	@Override
	public LinksAsserter<F, ? extends AbstractRoleAsserter<F,RA>, RA> links() {
		LinksAsserter<F, AbstractRoleAsserter<F,RA>, RA> asserter = new LinksAsserter<>(this, getDetails());
		copySetupTo(asserter);
		return asserter;
	}
	
	@Override
	public AbstractRoleAsserter<F,RA> assertLinks(int expected) {
		super.assertLinks(expected);
		return this;
	}
	
	@Override
	public AssignmentsAsserter<F, ? extends AbstractRoleAsserter<F,RA>, RA> assignments() {
		AssignmentsAsserter<F, AbstractRoleAsserter<F,RA>, RA> asserter = new AssignmentsAsserter<>(this, getDetails());
		copySetupTo(asserter);
		return asserter;
	}

	@Override
	public AbstractRoleAsserter<F,RA> assertLocality(String expectedOrig) {
		super.assertLocality(expectedOrig);
		return this;
	}
	
	public AbstractRoleAsserter<F,RA> assertDisplayName(String expectedOrig) {
		assertPolyStringProperty(OrgType.F_DISPLAY_NAME, expectedOrig);
		return this;
	}
	
	public AbstractRoleAsserter<F,RA> assertIdentifier(String expectedOrig) {
		assertPropertyEquals(OrgType.F_IDENTIFIER, expectedOrig);
		return this;
	}
		
	public AbstractRoleAsserter<F,RA> assertTenant(Boolean expected) {
		assertPropertyEquals(OrgType.F_TENANT, expected);
		return this;
	}
	
	public AbstractRoleAsserter<F,RA> assertIsTenant() {
		assertPropertyEquals(OrgType.F_TENANT, true);
		return this;
	}
	
	@Override
	public AbstractRoleAsserter<F,RA> assertHasProjectionOnResource(String resourceOid) throws ObjectNotFoundException, SchemaException {
		super.assertHasProjectionOnResource(resourceOid);
		return this;
	}
	
	@Override
	public ShadowAsserter<? extends AbstractRoleAsserter<F,RA>> projectionOnResource(String resourceOid) throws ObjectNotFoundException, SchemaException {
		return super.projectionOnResource(resourceOid);
	}
	
	@Override
	public AbstractRoleAsserter<F,RA> displayWithProjections() throws ObjectNotFoundException, SchemaException {
		super.displayWithProjections();
		return this;
	}

	@Override
	public ShadowReferenceAsserter<? extends AbstractRoleAsserter<F,RA>> singleLink() {
		return (ShadowReferenceAsserter<AbstractRoleAsserter<F,RA>>) super.singleLink();
	}

	@Override
	public AbstractRoleAsserter<F,RA> assertAssignments(int expected) {
		super.assertAssignments(expected);
		return this;
	}
	
	@Override
	public ParentOrgRefsAsserter<F, ? extends AbstractRoleAsserter<F,RA>, RA> parentOrgRefs() {
		ParentOrgRefsAsserter<F, AbstractRoleAsserter<F,RA>, RA> asserter = new ParentOrgRefsAsserter<>(this, getDetails());
		copySetupTo(asserter);
		return asserter;
	}
	
	@Override
	public AbstractRoleAsserter<F,RA> assertParentOrgRefs(String... expectedOids) {
		super.assertParentOrgRefs(expectedOids);
		return this;
	}
	
	@Override
	public RoleMembershipRefsAsserter<F, ? extends AbstractRoleAsserter<F,RA>, RA> roleMembershipRefs() {
		RoleMembershipRefsAsserter<F,AbstractRoleAsserter<F,RA>,RA> asserter = new RoleMembershipRefsAsserter<>(this, getDetails());
		copySetupTo(asserter);
		return asserter;
	}
	
	@Override
	public AbstractRoleAsserter<F,RA> assertRoleMemberhipRefs(int expected) {
		super.assertRoleMemberhipRefs(expected);
		return this;
	}
	
	@Override
	public ExtensionAsserter<F, ? extends  AbstractRoleAsserter<F,RA>, RA> extension() {
		ExtensionAsserter<F, ? extends AbstractRoleAsserter<F,RA>, RA> asserter = new ExtensionAsserter<>(this, getDetails());
		copySetupTo(asserter);
		return asserter;
	}
	
	@Override
	public TriggersAsserter<F, ? extends AbstractRoleAsserter<F,RA>, RA> triggers() {
		TriggersAsserter<F, ? extends AbstractRoleAsserter<F,RA>, RA> asserter = new TriggersAsserter<>(this, getDetails());
		copySetupTo(asserter);
		return asserter;
	}
	
	@Override
	public AbstractRoleAsserter<F,RA> assertNoItem(QName itemName) {
		super.assertNoItem(itemName);
		return this;
	}
	
	@Override
	public AbstractRoleAsserter<F,RA> assertNoItem(UniformItemPath itemPath) {
		super.assertNoItem(itemPath);
		return this;
	}
}
