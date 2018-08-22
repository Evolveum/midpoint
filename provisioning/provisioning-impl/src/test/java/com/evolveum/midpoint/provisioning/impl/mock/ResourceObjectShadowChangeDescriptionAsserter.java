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
package com.evolveum.midpoint.provisioning.impl.mock;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.asserter.ObjectDeltaAsserter;
import com.evolveum.midpoint.test.asserter.ShadowAsserter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author semancik
 *
 */
public class ResourceObjectShadowChangeDescriptionAsserter {
	
	private ResourceObjectShadowChangeDescription changeDesc;

	public ResourceObjectShadowChangeDescriptionAsserter(ResourceObjectShadowChangeDescription changeDesc) {
		this.changeDesc = changeDesc;
	}
	
	public ResourceObjectShadowChangeDescriptionAsserter display() {
		IntegrationTestTools.display("Change notification", changeDesc);
		return this;
	}
	
	public ObjectDeltaAsserter<ShadowType,ResourceObjectShadowChangeDescriptionAsserter> delta() {
		ObjectDelta<ShadowType> objectDelta = changeDesc.getObjectDelta();
		assertNotNull("No object delta in change notification", objectDelta);
		return new ObjectDeltaAsserter<>(objectDelta, this, "object delta in change notification");
	}
	
	public ResourceObjectShadowChangeDescriptionAsserter assertNoDelta() {
		assertNull("Unexpected object delta in change notificaiton", changeDesc.getObjectDelta());
		return this;
	}
	
	public ShadowAsserter<ResourceObjectShadowChangeDescriptionAsserter> currentShadow() {
		PrismObject<ShadowType> currentShadow = changeDesc.getCurrentShadow();
		assertNotNull("No current shadow in change notification", currentShadow);
		return new ShadowAsserter<>(currentShadow, this, "currentShadow in change notification");
	}
	
	public ResourceObjectShadowChangeDescriptionAsserter assertNoCurrentShadow() {
		assertNull("Unexpected current shadow in change notificaiton", changeDesc.getCurrentShadow());
		return this;
	}
	
	public ShadowAsserter<ResourceObjectShadowChangeDescriptionAsserter> oldShadow() {
		PrismObject<ShadowType> oldShadow = changeDesc.getOldShadow();
		assertNotNull("No old shadow in change notification", oldShadow);
		return new ShadowAsserter<>(oldShadow, this, "oldShadow in change notification");
	}
	
	public ResourceObjectShadowChangeDescriptionAsserter assertNoOldShadow() {
		assertNull("Unexpected old shadow in change notificaiton", changeDesc.getOldShadow());
		return this;
	}
	
	public ResourceObjectShadowChangeDescriptionAsserter assertUnrelatedChange(boolean expected) {
		assertEquals("Wrong unrelated change flag in change notification", expected, changeDesc.isUnrelatedChange());
		return this;
	}
	
	public ResourceObjectShadowChangeDescriptionAsserter assertProtected(boolean expected) {
		assertEquals("Wrong protected flag in change notification", expected, changeDesc.isProtected());
		return this;
	}

}
