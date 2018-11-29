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

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author semancik
 *
 */
public class ObjectDeltaAsserter<O extends ObjectType,RA> extends AbstractAsserter<RA> {
	
	private ObjectDelta<O> delta;

	public ObjectDeltaAsserter(ObjectDelta<O> delta) {
		super();
		this.delta = delta;
	}
	
	public ObjectDeltaAsserter(ObjectDelta<O> delta, String detail) {
		super(detail);
		this.delta = delta;
	}
	
	public ObjectDeltaAsserter(ObjectDelta<O> delta, RA returnAsserter, String detail) {
		super(returnAsserter, detail);
		this.delta = delta;
	}
	
	public static <O extends ObjectType> ObjectDeltaAsserter<O,Void> forDelta(ObjectDelta<O> delta) {
		return new ObjectDeltaAsserter<>(delta);
	}
	
	public ObjectDeltaAsserter<O,RA> assertAdd() {
		assertChangeType(ChangeType.ADD);
		return this;
	}
	
	public ObjectDeltaAsserter<O,RA> assertModify() {
		assertChangeType(ChangeType.MODIFY);
		return this;
	}
	
	public ObjectDeltaAsserter<O,RA> assertDelete() {
		assertChangeType(ChangeType.DELETE);
		return this;
	}
	
	public ObjectDeltaAsserter<O,RA> assertChangeType(ChangeType expected) {
		assertEquals("Wrong change type in "+desc(), expected, delta.getChangeType());
		return this;
	}

	public ObjectDeltaAsserter<O,RA> assertObjectTypeClass(Class<ShadowType> expected) {
		assertEquals("Wrong object type class in "+desc(), expected, delta.getObjectTypeClass());
		return this;
	}

	public ObjectDeltaAsserter<O,RA> assertOid(String expected) {
		assertEquals("Wrong OID in "+desc(), expected, delta.getOid());
		return this;
	}

	public ObjectDeltaAsserter<O,RA> assertOid() {
		assertNotNull("No OID in "+desc(), delta.getOid());
		return this;
	}
	
	public <C extends Containerable> ContainerDeltaAsserter<C,ObjectDeltaAsserter<O,RA>> container(QName qname) {
		return container((ItemPath) ItemName.fromQName(qname));
	}
	
	public <C extends Containerable> ContainerDeltaAsserter<C,ObjectDeltaAsserter<O,RA>> container(ItemPath path) {
		ContainerDelta<C> containerDelta = delta.findContainerDelta(path);
		assertNotNull("No container delta for path "+path+" in "+desc(), containerDelta);
		ContainerDeltaAsserter<C,ObjectDeltaAsserter<O,RA>> containerDeltaAsserter = new ContainerDeltaAsserter<>(containerDelta, this, "container delta for "+path+" in "+desc());
		copySetupTo(containerDeltaAsserter);
		return containerDeltaAsserter;
	}
	
	protected String desc() {
		return descWithDetails(delta);
	}

	public ObjectDeltaAsserter<O,RA> display() {
		display(desc());
		return this;
	}
	
	public ObjectDeltaAsserter<O,RA> display(String message) {
		IntegrationTestTools.display(message, delta);
		return this;
	}
}
