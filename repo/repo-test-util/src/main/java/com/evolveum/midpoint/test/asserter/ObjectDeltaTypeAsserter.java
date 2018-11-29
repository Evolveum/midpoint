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

import com.evolveum.midpoint.prism.path.UniformItemPath;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ItemDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

/**
 * @author semancik
 *
 */
public class ObjectDeltaTypeAsserter<RA> extends AbstractAsserter<RA> {
	
	private ObjectDeltaType delta;

	public ObjectDeltaTypeAsserter(ObjectDeltaType delta) {
		super();
		this.delta = delta;
	}
	
	public ObjectDeltaTypeAsserter(ObjectDeltaType delta, String details) {
		super(details);
		this.delta = delta;
	}
	
	public ObjectDeltaTypeAsserter(ObjectDeltaType delta, RA returnAsserter, String details) {
		super(returnAsserter, details);
		this.delta = delta;
	}
	
	public static ObjectDeltaTypeAsserter<Void> forDelta(ObjectDeltaType delta) {
		return new ObjectDeltaTypeAsserter<>(delta);
	}
	
	public static ObjectDeltaTypeAsserter<Void> forDelta(ObjectDeltaType delta, String details) {
		return new ObjectDeltaTypeAsserter<>(delta, details);
	}
	
	public ObjectDeltaTypeAsserter<RA> assertAdd() {
		assertChangeType(ChangeTypeType.ADD);
		return this;
	}
	
	public ObjectDeltaTypeAsserter<RA> assertModify() {
		assertChangeType(ChangeTypeType.MODIFY);
		return this;
	}
	
	public ObjectDeltaTypeAsserter<RA> assertDelete() {
		assertChangeType(ChangeTypeType.DELETE);
		return this;
	}
	
	public ObjectDeltaTypeAsserter<RA> assertChangeType(ChangeTypeType expected) {
		assertEquals("Wrong change type in "+desc(), expected, delta.getChangeType());
		return this;
	}
	
	public ObjectDeltaTypeAsserter<RA> assertHasModification(UniformItemPath itemPath) {
		for (ItemDeltaType itemDelta: delta.getItemDelta()) {
			if (itemPath.equivalent(itemDelta.getPath().getItemPath())) {
				return this;
			}
		}
		fail("No modification for "+itemPath+" in "+desc());
		return null; // not reached
	}
	
	protected String desc() {
		return descWithDetails(delta);
	}
	
	public ObjectDeltaTypeAsserter<RA> display() {
		if (getDetails() != null) {
			display(getDetails());
		} else {
			display("ObjectDeltaType");
		}
		return this;
	}
	
	public ObjectDeltaTypeAsserter<RA> display(String message) {
		IntegrationTestTools.display(message, PrettyPrinter.debugDump(delta, 1));
		return this;
	}
}
