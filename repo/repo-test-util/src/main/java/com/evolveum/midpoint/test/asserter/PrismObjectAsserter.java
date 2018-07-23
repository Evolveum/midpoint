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
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author semancik
 *
 */
public class PrismObjectAsserter<O extends ObjectType> extends AbstractAsserter {
	
	private PrismObject<O> object;

	public PrismObjectAsserter(PrismObject<O> object) {
		super();
		this.object = object;
	}
	
	public PrismObjectAsserter(PrismObject<O> object, String details) {
		super(details);
		this.object = object;
	}
	
	public PrismObject<O> getObject() {
		return object;
	}

	public static <O extends ObjectType> PrismObjectAsserter<O> forObject(PrismObject<O> shadow) {
		return new PrismObjectAsserter<>(shadow);
	}
	
	public static <O extends ObjectType> PrismObjectAsserter<O> forObject(PrismObject<O> shadow, String details) {
		return new PrismObjectAsserter<>(shadow, details);
	}
	
	public PrismObjectAsserter<O> assertOid() {
		assertNotNull("No OID in "+desc(), getObject().getOid());
		return this;
	}
	
	public PrismObjectAsserter<O> assertOid(String expected) {
		assertEquals("Wrong OID in "+desc(), expected, getObject().getOid());
		return this;
	}
	
	public PrismObjectAsserter<O> assertName() {
		assertNotNull("No name in "+desc(), getObject().getName());
		return this;
	}
	
	public PrismObjectAsserter<O> assertName(String expectedOrig) {
		PrismAsserts.assertEqualsPolyString("Wrong name in "+desc(), expectedOrig, getObject().getName());
		return this;
	}
	
	public PrismObjectAsserter<O> assertLifecycleState(String expected) {
		assertEquals("Wrong lifecycleState in "+desc(), expected, getObject().asObjectable().getLifecycleState());
		return this;
	}
	
	public PrismObjectAsserter<O> assertActiveLifecycleState() {
		String actualLifecycleState = getObject().asObjectable().getLifecycleState();
		if (actualLifecycleState != null) {
			assertEquals("Wrong lifecycleState in "+desc(), SchemaConstants.LIFECYCLE_ACTIVE, actualLifecycleState);
		}
		return this;
	}
	
	protected String desc() {
		return descWithDetails(object);
	}
	
	public PrismObjectAsserter<O> display() {
		display(desc());
		return this;
	}
	
	public PrismObjectAsserter<O> display(String message) {
		IntegrationTestTools.display(message, object);
		return this;
	}
}
