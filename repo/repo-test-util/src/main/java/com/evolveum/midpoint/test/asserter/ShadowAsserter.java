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

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationType;
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
	
	public PendingOperationsAsserter pendingOperations() {
		return new PendingOperationsAsserter(getObject(), getDetails());
	}
	
	public ShadowAttributesAsserter attributes() {
		return new ShadowAttributesAsserter(getObject(), getDetails());
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
