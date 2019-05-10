/**
 * Copyright (c) 2018-2019 Evolveum
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

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

/**
 * @author semancik
 *
 */
public class AssignmentAsserter<R> extends AbstractAsserter<R> {
	
	final private AssignmentType assignment;
	private PrismObject<?> resolvedTarget = null;

	public AssignmentAsserter(AssignmentType assignment) {
		super();
		this.assignment = assignment;
	}
	
	public AssignmentAsserter(AssignmentType assignment, String detail) {
		super(detail);
		this.assignment = assignment;
	}
	
	public AssignmentAsserter(AssignmentType assignment, PrismObject<?> resolvedTarget, R returnAsserter, String detail) {
		super(returnAsserter, detail);
		this.assignment = assignment;
		this.resolvedTarget = resolvedTarget;
	}
	
	protected AssignmentType getAssignment() {
		return assignment;
	}
	
	public String getTargetOid() {
		return getAssignment().getTargetRef().getOid();
	}
	
	public AssignmentAsserter<R> assertTargetOid() {
		assertNotNull("No target OID in "+desc(), getTargetOid());
		return this;
	}
	
	public AssignmentAsserter<R> assertTargetOid(String expected) {
		assertEquals("Wrong target OID in "+desc(), expected, getTargetOid());
		return this;
	}
	
	public AssignmentAsserter<R> assertTargetType(QName expected) {
		assertEquals("Wrong target type in "+desc(), expected, getAssignment().getTargetRef().getType());
		return this;
	}
	
	public AssignmentAsserter<R> assertRole(String expectedOid) {
		assertTargetOid(expectedOid);
		assertTargetType(RoleType.COMPLEX_TYPE);
		return this;
	}
	
	public AssignmentAsserter<R> assertSubtype(String expected) {
		List<String> subtypes = assignment.getSubtype();
		if (subtypes.isEmpty()) {
			fail("No subtypes in "+desc()+", expected "+expected);
		}
		if (subtypes.size() > 1) {
			fail("Too many subtypes in "+desc()+", expected "+expected+", was "+subtypes);
		}
		assertEquals("Wrong subtype in "+desc(), expected, subtypes.get(0));
		return this;
	}
	
	public ActivationAsserter<AssignmentAsserter<R>> activation() {
		ActivationAsserter<AssignmentAsserter<R>> asserter = new ActivationAsserter<>(assignment.getActivation(), this, getDetails());
		copySetupTo(asserter);
		return asserter;
	}
	
	public MetadataAsserter<AssignmentAsserter<R>> metadata() {
		MetadataAsserter<AssignmentAsserter<R>> asserter = new MetadataAsserter<>(assignment.getMetadata(), this, getDetails());
		copySetupTo(asserter);
		return asserter;
	}
	
	protected String desc() {
		// TODO: better desc
		return descWithDetails(assignment);
	}
	
	public AssignmentAsserter<R> display() {
		display(desc());
		return this;
	}
	
	public AssignmentAsserter<R> display(String message) {
		IntegrationTestTools.display(message, assignment);
		return this;
	}	
}
