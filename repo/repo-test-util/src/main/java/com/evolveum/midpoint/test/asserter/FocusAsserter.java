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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.testng.AssertJUnit;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author semancik
 *
 */
public class FocusAsserter<F extends FocusType,R> extends PrismObjectAsserter<F,R> {
	
	private Map<String,PrismObject<ShadowType>> projectionCache = new HashMap<>();
	
	public FocusAsserter(PrismObject<F> focus) {
		super(focus);
	}
	
	public FocusAsserter(PrismObject<F> focus, String details) {
		super(focus, details);
	}
	
	public FocusAsserter(PrismObject<F> focus, R returnAsserter, String details) {
		super(focus, returnAsserter, details);
	}
	
	public static <F extends FocusType> FocusAsserter<F,Void> forFocus(PrismObject<F> focus) {
		return new FocusAsserter<>(focus);
	}
	
	public static <F extends FocusType> FocusAsserter<F,Void> forFocus(PrismObject<F> focus, String details) {
		return new FocusAsserter<>(focus, details);
	}
	
	@Override
	public FocusAsserter<F,R> assertOid() {
		super.assertOid();
		return this;
	}
	
	@Override
	public FocusAsserter<F,R> assertOid(String expected) {
		super.assertOid(expected);
		return this;
	}
	
	@Override
	public FocusAsserter<F,R> assertOidDifferentThan(String oid) {
		super.assertOidDifferentThan(oid);
		return this;
	}
	
	@Override
	public FocusAsserter<F,R> assertName() {
		super.assertName();
		return this;
	}
	
	@Override
	public FocusAsserter<F,R> assertName(String expectedOrig) {
		super.assertName(expectedOrig);
		return this;
	}
	
	@Override
	public FocusAsserter<F,R> assertLifecycleState(String expected) {
		super.assertLifecycleState(expected);
		return this;
	}
	
	@Override
	public FocusAsserter<F,R> assertActiveLifecycleState() {
		super.assertActiveLifecycleState();
		return this;
	}
	
	public FocusAsserter<F,R> assertAdministrativeStatus(ActivationStatusType expected) {
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
	
	public FocusAsserter<F,R> display() {
		super.display();
		return this;
	}
	
	public FocusAsserter<F,R> display(String message) {
		super.display(message);
		return this;
	}
	
	public FocusAsserter<F,R> assertLinks(int expected) {
		PrismReference linkRef = getObject().findReference(FocusType.F_LINK_REF);
		if (linkRef == null) {
			assertTrue("Expected "+expected+" links, but there is no linkRef in "+desc(), expected == 0);
			return this;
		}
		assertEquals("Wrong number of links in " + desc(), expected, linkRef.size());
		return this;
	}
	
	public ShadowReferenceAsserter<FocusAsserter<F,R>> singleLink() {
		PrismReference linkRef = getObject().findReference(FocusType.F_LINK_REF);
		if (linkRef == null) {
			fail("Expected single link, but is no linkRef in "+desc());
			return null; // not reached
		}
		assertEquals("Wrong number of links in " + desc(), 1, linkRef.size());
		ShadowReferenceAsserter<FocusAsserter<F, R>> asserter = new ShadowReferenceAsserter<>(linkRef.getValue(0), this, "link in "+desc());
		copySetupTo(asserter);
		return asserter;
	}
	
	public FocusAsserter<F,R> assertHasProjectionOnResource(String resourceOid) throws ObjectNotFoundException, SchemaException {
		PrismObject<ShadowType> shadow = findProjectionOnResource(resourceOid);
		assertNotNull("Projection for resource "+resourceOid+" not found in "+desc(), shadow);
        return this;
	}
	
	public <A extends FocusAsserter<F,R>> ShadowAsserter<A> projectionOnResource(String resourceOid) throws ObjectNotFoundException, SchemaException {
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

	private List<PrismObject<ShadowType>> getLinkTargets() throws ObjectNotFoundException, SchemaException {
		F focusType = getObject().asObjectable();
		List<PrismObject<ShadowType>> shadows = new ArrayList<>();
        for (ObjectReferenceType linkRefType: focusType.getLinkRef()) {
        	String linkTargetOid = linkRefType.getOid();
	        assertFalse("No linkRef oid in "+desc(), StringUtils.isBlank(linkTargetOid));
	        shadows.add(getLinkTarget(linkTargetOid));
        }
        return shadows;
	}

	private PrismObject<ShadowType> getLinkTarget(String oid) throws ObjectNotFoundException, SchemaException {
		PrismObject<ShadowType> shadow = projectionCache.get(oid);
		if (shadow == null) {
			shadow = resolveObject(ShadowType.class, oid);
			projectionCache.put(oid, shadow);
		}
		return shadow;
	}
}
