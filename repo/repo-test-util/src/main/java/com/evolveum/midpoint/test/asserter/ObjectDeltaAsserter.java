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

import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author semancik
 *
 */
public class ObjectDeltaAsserter<O extends ObjectType,R> extends AbstractAsserter<R> {
	
	private ObjectDelta<O> delta;

	public ObjectDeltaAsserter(ObjectDelta<O> delta) {
		super();
		this.delta = delta;
	}
	
	public ObjectDeltaAsserter(ObjectDelta<O> delta, String detail) {
		super(detail);
		this.delta = delta;
	}
	
	public ObjectDeltaAsserter(ObjectDelta<O> delta, R returnAsserter, String detail) {
		super(returnAsserter, detail);
		this.delta = delta;
	}
	
	public static <O extends ObjectType> ObjectDeltaAsserter<O,Void> forDelta(ObjectDelta<O> delta) {
		return new ObjectDeltaAsserter<>(delta);
	}
	
	public ObjectDeltaAsserter<O,R> assertAdd() {
		assertChangeType(ChangeType.ADD);
		return this;
	}
	
	public ObjectDeltaAsserter<O,R> assertModify() {
		assertChangeType(ChangeType.MODIFY);
		return this;
	}
	
	public ObjectDeltaAsserter<O,R> assertDelete() {
		assertChangeType(ChangeType.DELETE);
		return this;
	}
	
	public ObjectDeltaAsserter<O,R> assertChangeType(ChangeType expected) {
		assertEquals("Wrong change type in "+desc(), expected, delta.getChangeType());
		return this;
	}
	
	private String desc() {
		return descWithDetails(delta);
	}
}
