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

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.path.UniformItemPath;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author semancik
 *
 */
public class FocusAsserter<F extends FocusType,RA> extends PrismObjectAsserter<F,RA> {
	
	public FocusAsserter(PrismObject<F> focus) {
		super(focus);
	}
	
	public FocusAsserter(PrismObject<F> focus, String details) {
		super(focus, details);
	}
	
	public FocusAsserter(PrismObject<F> focus, RA returnAsserter, String details) {
		super(focus, returnAsserter, details);
	}
	
	public static <F extends FocusType> FocusAsserter<F,Void> forFocus(PrismObject<F> focus) {
		return new FocusAsserter<>(focus);
	}
	
	public static <F extends FocusType> FocusAsserter<F,Void> forFocus(PrismObject<F> focus, String details) {
		return new FocusAsserter<>(focus, details);
	}

	// ::::::::::::::::::::::::::::::::::::::::::
	// : NOTE : WARNING : ATTENTION : LOOK HERE :
	// ::::::::::::::::::::::::::::::::::::::::::
	//
	// If you add any method here, add it also in UserAsserter, OrgAsserter and other subclasses.
	//
	// It is insane to override all those methods from superclass.
	// But there is no better way to specify something like <SELF> type in Java.
	// This is lesser evil.
	
	@Override
	public FocusAsserter<F,RA> assertOid() {
		super.assertOid();
		return this;
	}
	
	@Override
	public FocusAsserter<F,RA> assertOid(String expected) {
		super.assertOid(expected);
		return this;
	}
	
	@Override
	public FocusAsserter<F,RA> assertOidDifferentThan(String oid) {
		super.assertOidDifferentThan(oid);
		return this;
	}
	
	@Override
	public FocusAsserter<F,RA> assertName() {
		super.assertName();
		return this;
	}
	
	@Override
	public FocusAsserter<F,RA> assertName(String expectedOrig) {
		super.assertName(expectedOrig);
		return this;
	}
	
	@Override
	public FocusAsserter<F,RA> assertDescription(String expected) {
		super.assertDescription(expected);
		return this;
	}
	
	@Override
	public FocusAsserter<F,RA> assertSubtype(String... expected) {
		super.assertSubtype(expected);
		return this;
	}
	
	@Override
	public FocusAsserter<F,RA> assertTenantRef(String expectedOid) {
		super.assertTenantRef(expectedOid);
		return this;
	}
	
	public FocusAsserter<F,RA> assertLocality(String expectedOrig) {
		assertPolyStringProperty(OrgType.F_LOCALITY, expectedOrig);
		return this;
	}
	
	@Override
	public FocusAsserter<F,RA> assertLifecycleState(String expected) {
		super.assertLifecycleState(expected);
		return this;
	}
	
	@Override
	public FocusAsserter<F,RA> assertActiveLifecycleState() {
		super.assertActiveLifecycleState();
		return this;
	}
	
	public ActivationAsserter<F, ? extends FocusAsserter<F,RA>, RA> activation() {
		ActivationAsserter<F,FocusAsserter<F,RA>,RA> asserter = new ActivationAsserter<>(this, getDetails());
		copySetupTo(asserter);
		return asserter;
	}
	
	public FocusAsserter<F,RA> assertAdministrativeStatus(ActivationStatusType expected) {
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
	
	public FocusAsserter<F,RA> display() {
		super.display();
		return this;
	}
	
	public FocusAsserter<F,RA> display(String message) {
		super.display(message);
		return this;
	}
	
	public LinksAsserter<F, ? extends FocusAsserter<F,RA>, RA> links() {
		LinksAsserter<F,FocusAsserter<F,RA>,RA> asserter = new LinksAsserter<>(this, getDetails());
		copySetupTo(asserter);
		return asserter;
	}
	
	public FocusAsserter<F,RA> assertLinks(int expected) {
		links().assertLinks(expected);
		return this;
	}
	
	public ShadowReferenceAsserter<? extends FocusAsserter<F,RA>> singleLink() {
		PrismReference linkRef = getObject().findReference(FocusType.F_LINK_REF);
		if (linkRef == null) {
			fail("Expected single link, but is no linkRef in "+desc());
			return null; // not reached
		}
		assertEquals("Wrong number of links in " + desc(), 1, linkRef.size());
		ShadowReferenceAsserter<FocusAsserter<F, RA>> asserter = new ShadowReferenceAsserter<>(linkRef.getValue(0), null, this, "link in "+desc());
		copySetupTo(asserter);
		return asserter;
	}
	
	@Override
	public ParentOrgRefsAsserter<F, ? extends FocusAsserter<F,RA>, RA> parentOrgRefs() {
		ParentOrgRefsAsserter<F,FocusAsserter<F,RA>,RA> asserter = new ParentOrgRefsAsserter<>(this, getDetails());
		copySetupTo(asserter);
		return asserter;
	}
	
	@Override
	public FocusAsserter<F,RA> assertParentOrgRefs(String... expectedOids) {
		super.assertParentOrgRefs(expectedOids);
		return this;
	}
	
	public FocusAsserter<F,RA> assertHasProjectionOnResource(String resourceOid) throws ObjectNotFoundException, SchemaException {
		PrismObject<ShadowType> shadow = findProjectionOnResource(resourceOid);
		assertNotNull("Projection for resource "+resourceOid+" not found in "+desc(), shadow);
        return this;
	}
	
	public <A extends FocusAsserter<F,RA>> ShadowAsserter<A> projectionOnResource(String resourceOid) throws ObjectNotFoundException, SchemaException {
		PrismObject<ShadowType> shadow = findProjectionOnResource(resourceOid);
		assertNotNull("Projection for resource "+resourceOid+" not found in "+desc(), shadow);
		ShadowAsserter<A> asserter = new ShadowAsserter<A>(shadow, (A)this, "projection of "+desc());
		copySetupTo(asserter);
		return asserter;
	}
	
	private PrismObject<ShadowType> findProjectionOnResource(String resourceOid) throws ObjectNotFoundException, SchemaException {
		F focusType = getObject().asObjectable();
        for (PrismObject<ShadowType> shadow: getLinkTargets()) {
	        if (resourceOid.equals(shadow.asObjectable().getResourceRef().getOid())) {
	        	return shadow;
	        }
        }
        return null;
	}
	
	public FocusAsserter<F,RA> assertLinked(String targetOid) throws ObjectNotFoundException, SchemaException {
		F focusType = getObject().asObjectable();
		for (ObjectReferenceType linkRefType: focusType.getLinkRef()) {
			if (targetOid.equals(linkRefType.getOid())) {
				return this;
			}
		}
        fail("Target "+targetOid+" not linked to "+desc());
        return null; // not reached
	}

	private List<PrismObject<ShadowType>> getLinkTargets() throws ObjectNotFoundException, SchemaException {
		F focusType = getObject().asObjectable();
		List<PrismObject<ShadowType>> shadows = new ArrayList<>();
        for (ObjectReferenceType linkRefType: focusType.getLinkRef()) {
        	String linkTargetOid = linkRefType.getOid();
	        assertFalse("No linkRef oid in "+desc(), StringUtils.isBlank(linkTargetOid));
	        shadows.add(getCachedObject(ShadowType.class, linkTargetOid));
        }
        return shadows;
	}

	
	
	public  FocusAsserter<F,RA> displayWithProjections() throws ObjectNotFoundException, SchemaException {
		StringBuilder sb = new StringBuilder();
		List<ObjectReferenceType> linkRefs = getObject().asObjectable().getLinkRef();
		sb.append(getObject()).append(" ").append(linkRefs.size()).append(" projections:");
		for (ObjectReferenceType linkRefType: linkRefs) {
			sb.append("\n  ");
			String linkTargetOid = linkRefType.getOid();
	        assertFalse("No linkRef oid in "+desc(), StringUtils.isBlank(linkTargetOid));
	        try {
		        PrismObject<ShadowType> linkTarget = getCachedObject(ShadowType.class, linkTargetOid);
				sb.append(linkTarget);
				ShadowType shadowType = linkTarget.asObjectable();
				ObjectReferenceType resourceRef = shadowType.getResourceRef();
				sb.append(", resource=").append(resourceRef.getOid());
				appendFlag(sb, "dead", shadowType.isDead());
				appendFlag(sb, "exists", shadowType.isExists());
				List<PendingOperationType> pendingOperations = shadowType.getPendingOperation();
				if (!pendingOperations.isEmpty()) {
					sb.append(", ").append(pendingOperations.size()).append(" pending operations");
				}
	        } catch (ObjectNotFoundException e) {
	        	sb.append("shadow(").append(linkTargetOid).append(": NOT FOUND");
	        } catch (Exception e) {
	        	sb.append("shadow(").append(linkTargetOid).append("): ERROR(")
	        		.append(e.getClass().getSimpleName()).append("): ").append(e.getMessage());
	        }
			
		}
		IntegrationTestTools.display(desc(), sb.toString());
		return this;
	}
	
	private void appendFlag(StringBuilder sb, String label, Boolean val) {
		if (val != null) {
			sb.append(", ").append(label).append("=").append(val);
		}	
	}

	public AssignmentsAsserter<F, ? extends FocusAsserter<F,RA>, RA> assignments() {
		AssignmentsAsserter<F,FocusAsserter<F,RA>,RA> asserter = new AssignmentsAsserter<>(this, getDetails());
		copySetupTo(asserter);
		return asserter;
	}
	
	public FocusAsserter<F,RA> assertAssignments(int expected) {
		assignments().assertAssignments(expected);
		return this;
	}
	
	public RoleMembershipRefsAsserter<F, ? extends FocusAsserter<F,RA>, RA> roleMembershipRefs() {
		RoleMembershipRefsAsserter<F,FocusAsserter<F,RA>,RA> asserter = new RoleMembershipRefsAsserter<>(this, getDetails());
		copySetupTo(asserter);
		return asserter;
	}
	
	public FocusAsserter<F,RA> assertRoleMemberhipRefs(int expected) {
		roleMembershipRefs().assertRoleMemberhipRefs(expected);
		return this;
	}
	
	@Override
	public ExtensionAsserter<F, ? extends FocusAsserter<F,RA>, RA> extension() {
		ExtensionAsserter<F, ? extends FocusAsserter<F,RA>, RA> asserter = new ExtensionAsserter<>(this, getDetails());
		copySetupTo(asserter);
		return asserter;
	}
	
	@Override
	public TriggersAsserter<F, ? extends FocusAsserter<F,RA>, RA> triggers() {
		TriggersAsserter<F, ? extends FocusAsserter<F,RA>, RA> asserter = new TriggersAsserter<>(this, getDetails());
		copySetupTo(asserter);
		return asserter;
	}
	
	@Override
	public FocusAsserter<F,RA> assertNoItem(QName itemName) {
		super.assertNoItem(itemName);
		return this;
	}
	
	@Override
	public FocusAsserter<F,RA> assertNoItem(UniformItemPath itemPath) {
		super.assertNoItem(itemPath);
		return this;
	}
}
