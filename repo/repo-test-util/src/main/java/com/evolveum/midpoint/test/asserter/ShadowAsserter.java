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
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author semancik
 *
 */
public class ShadowAsserter extends PrismObjectAsserter<ShadowType> {
	
	public ShadowAsserter(PrismObject<ShadowType> shadow) {
		super(shadow);
	}
	
	public ShadowAsserter(PrismObject<ShadowType> shadow, String details) {
		super(shadow, details);
	}
	
	public static ShadowAsserter forShadow(PrismObject<ShadowType> shadow) {
		return new ShadowAsserter(shadow);
	}
	
	public static ShadowAsserter forShadow(PrismObject<ShadowType> shadow, String details) {
		return new ShadowAsserter(shadow, details);
	}
	
	@Override
	public ShadowAsserter assertOid() {
		super.assertOid();
		return this;
	}
	
	@Override
	public ShadowAsserter assertOid(String expected) {
		super.assertOid(expected);
		return this;
	}
	
	@Override
	public ShadowAsserter assertName() {
		super.assertName();
		return this;
	}
	
	@Override
	public ShadowAsserter assertName(String expectedOrig) {
		super.assertName(expectedOrig);
		return this;
	}
	
	@Override
	public ShadowAsserter assertLifecycleState(String expected) {
		super.assertLifecycleState(expected);
		return this;
	}
	
	@Override
	public ShadowAsserter assertActiveLifecycleState() {
		super.assertActiveLifecycleState();
		return this;
	}
	
	public ShadowAsserter assertObjectClass() {
		assertNotNull("No objectClass in "+desc(), getObject().asObjectable().getObjectClass());
		return this;
	}
	
	public ShadowAsserter assertObjectClass(QName expected) {
		PrismAsserts.assertMatchesQName("Wrong objectClass in "+desc(), expected, getObject().asObjectable().getObjectClass());
		return this;
	}
	
	public ShadowAsserter assertKind() {
		assertNotNull("No kind in "+desc(), getObject().asObjectable().getKind());
		return this;
	}
	
	public ShadowAsserter assertKind(ShadowKindType expected) {
		assertEquals("Wrong kind in "+desc(), expected, getObject().asObjectable().getKind());
		return this;
	}
	
	public ShadowAsserter assertAdministrativeStatus(ActivationStatusType expected) {
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
	
	public ShadowAsserter assertBasicRepoProperties() {
		assertOid();
		assertName();
		assertObjectClass();
		attributes().assertAny();
		return this;
	}

	public ShadowAsserter assertDead() {
		assertIsDead(true);
		return this;
	}
	
	public ShadowAsserter assertNotDead() {
		Boolean isDead = getObject().asObjectable().isDead();
		if (isDead != null && isDead) {
			fail("Wrong isDead in "+desc()+", expected null or false, but was true");
		}
		return this;
	}
	
	public ShadowAsserter assertIsDead(Boolean expected) {
		assertEquals("Wrong isDead in "+desc(), expected, getObject().asObjectable().isDead());
		return this;
	}
	
	public ShadowAsserter assertIsExists() {
		Boolean isExists = getObject().asObjectable().isExists();
		if (isExists != null && !isExists) {
			fail("Wrong isExists in "+desc()+", expected null or true, but was false");
		}
		return this;
	}
	
	public ShadowAsserter assertIsNotExists() {
		assertIsExists(false);
		return this;
	}
	
	public ShadowAsserter assertIsExists(Boolean expected) {
		assertEquals("Wrong isExists in "+desc(), expected, getObject().asObjectable().isExists());
		return this;
	}
	
	public PendingOperationsAsserter pendingOperations() {
		return new PendingOperationsAsserter(this, getDetails());
	}
	
	public ShadowAttributesAsserter attributes() {
		return new ShadowAttributesAsserter(this, getDetails());
	}

	public ShadowAsserter assertNoLegacyConsistency() {
		PrismAsserts.assertNoItem(getObject(), ShadowType.F_RESULT);
		PrismAsserts.assertNoItem(getObject(), ShadowType.F_ATTEMPT_NUMBER);
		PrismAsserts.assertNoItem(getObject(), ShadowType.F_FAILED_OPERATION_TYPE);
		PrismAsserts.assertNoItem(getObject(), ShadowType.F_OBJECT_CHANGE);
		return this;
	}
	
	public ShadowAsserter display() {
		super.display();
		return this;
	}
	
	public ShadowAsserter display(String message) {
		super.display(message);
		return this;
	}

}
