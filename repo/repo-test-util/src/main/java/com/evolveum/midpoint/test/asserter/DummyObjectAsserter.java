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

import java.util.Arrays;
import java.util.Set;

import javax.xml.datatype.XMLGregorianCalendar;

import org.testng.AssertJUnit;

import com.evolveum.icf.dummy.resource.DummyObject;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

/**
 * @author semancik
 *
 */
public class DummyObjectAsserter<D extends DummyObject,R> extends AbstractAsserter<R> {
	
	private D dummyObject;
	private String dummyResourceName;

	public DummyObjectAsserter(D dummyObject, String dummyResourceName) {
		super();
		this.dummyObject = dummyObject;
		this.dummyResourceName = dummyResourceName;
	}
	
	public DummyObjectAsserter(D dummyObject, String dummyResourceName, String details) {
		super(details);
		this.dummyObject = dummyObject;
		this.dummyResourceName = dummyResourceName;
	}
	
	public DummyObjectAsserter(D dummyObject, String dummyResourceName, R returnAsserter, String details) {
		super(returnAsserter, details);
		this.dummyObject = dummyObject;
		this.dummyResourceName = dummyResourceName;
	}
	
	public D getDummyObject() {
		return dummyObject;
	}
	
	protected D getDummyObjectAssertExists() {
		D dummyObject = getDummyObject();
		assertNotNull(desc()+" does not exist", dummyObject);
		return dummyObject;
	}

	public DummyObjectAsserter<D,R> assertName(String expected) {
		assertEquals("Wrong name in "+desc(), expected, getDummyObjectAssertExists().getName());
		return this;
	}
	
	public <T> DummyObjectAsserter<D,R> assertAttribute(String attributeName, T... expectedAttributeValues) {
		
		Set<Object> values = getDummyObjectAssertExists().getAttributeValues(attributeName, Object.class);
		if ((values == null || values.isEmpty()) && (expectedAttributeValues == null || expectedAttributeValues.length == 0)) {
			return this;
		}
		assertNotNull("No values for attribute "+attributeName+" of "+desc(), values);
		assertEquals("Unexpected number of values for attribute " + attributeName + " of " + desc() +
				". Expected: " + Arrays.toString(expectedAttributeValues) + ", was: " + values,
				expectedAttributeValues.length, values.size());
		for (Object expectedValue: expectedAttributeValues) {
			if (!values.contains(expectedValue)) {
				AssertJUnit.fail("Value '"+expectedValue+"' expected in attribute "+attributeName+" of " + desc() +
						" but not found. Values found: "+values);
			}
		}
		return this;
	}
	
	public DummyObjectAsserter<D,R> assertEnabled() {
		assertTrue(desc() + " is disabled", getDummyObjectAssertExists().isEnabled());
		return this;
	}
	
	protected String desc() {
		if (dummyResourceName == null) {
			return descWithDetails(dummyObject) + " on default dummy resource";
		} else {
			return descWithDetails(dummyObject.toString()) + " on dummy resource '"+dummyResourceName+"'";
		}
	}
	
	public DummyObjectAsserter<D,R> display() {
		display(desc());
		return this;
	}
	
	public DummyObjectAsserter<D,R> display(String message) {
		IntegrationTestTools.display(message, dummyObject);
		return this;
	}
}
