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
import java.util.Collections;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;

/**
 * @author semancik
 *
 */
public class AssignmentsAsserter<F extends FocusType, FA extends FocusAsserter<F, RA>,RA> extends AbstractAsserter<FA> {
	
	private FA focusAsserter;
	private List<AssignmentType> assignments;

	public AssignmentsAsserter(FA focusAsserter) {
		super();
		this.focusAsserter = focusAsserter;
	}
	
	public AssignmentsAsserter(FA focusAsserter, String details) {
		super(details);
		this.focusAsserter = focusAsserter;
	}
	
	public static <F extends FocusType> AssignmentsAsserter<F,FocusAsserter<F,Void>,Void> forFocus(PrismObject<F> focus) {
		return new AssignmentsAsserter<>(FocusAsserter.forFocus(focus));
	}
	
	List<AssignmentType> getAssignments() {
		if (assignments == null) {
			assignments = getFocus().asObjectable().getAssignment();
		}
		return assignments;
	}
	
	public AssignmentsAsserter<F, FA, RA> assertAssignments(int expected) {
		assertEquals("Wrong number of assignments in " + desc(), expected, getAssignments().size());
		return this;
	}
	
	public AssignmentsAsserter<F, FA, RA> assertNone() {
		assertAssignments(0);
		return this;
	}
	
	AssignmentAsserter<AssignmentsAsserter<F, FA, RA>> forAssignment(AssignmentType assignment, PrismObject<?> target) {
		AssignmentAsserter<AssignmentsAsserter<F, FA, RA>> asserter = new AssignmentAsserter<>(assignment, target, this, "assignment in "+desc());
		copySetupTo(asserter);
		return asserter;
	}

	public AssignmentAsserter<AssignmentsAsserter<F, FA, RA>> single() {
		assertAssignments(1);
		return forAssignment(getAssignments().get(0), null);
	}
	
	PrismObject<F> getFocus() {
		return focusAsserter.getObject();
	}
	
	@Override
	public FA end() {
		return focusAsserter;
	}

	@Override
	protected String desc() {
		return descWithDetails("assignments of "+getFocus());
	}
	
	public AssignmentFinder<F,FA,RA> by() {
		return new AssignmentFinder<>(this);
	}
	
	public AssignmentAsserter<AssignmentsAsserter<F,FA,RA>> forRole(String roleOid) throws ObjectNotFoundException, SchemaException {
		return by()
			.targetOid(roleOid)
			.targetType(RoleType.COMPLEX_TYPE)
			.find();
	}
	
	public AssignmentsAsserter<F,FA,RA> assertRole(String roleOid) throws ObjectNotFoundException, SchemaException {
		by()
			.targetOid(roleOid)
			.targetType(RoleType.COMPLEX_TYPE)
			.find();
		return this;
	}
	
	public AssignmentsAsserter<F,FA,RA> assertRole(String roleOid, QName relation) throws ObjectNotFoundException, SchemaException {
		by()
			.targetOid(roleOid)
			.targetType(RoleType.COMPLEX_TYPE)
			.targetRelation(relation)
			.find();
		return this;
	}
	
	public AssignmentsAsserter<F,FA,RA> assertNoRole(String roleOid) throws ObjectNotFoundException, SchemaException {
		by()
			.targetOid(roleOid)
			.targetType(RoleType.COMPLEX_TYPE)
			.assertNone();
		return this;
	}
	
	public AssignmentsAsserter<F,FA,RA> assertNoRole() throws ObjectNotFoundException, SchemaException {
		by()
			.targetType(RoleType.COMPLEX_TYPE)
			.assertNone();
		return this;
	}
	
	public AssignmentsAsserter<F,FA,RA> assertOrg(String orgOid) throws ObjectNotFoundException, SchemaException {
		by()
			.targetOid(orgOid)
			.targetType(OrgType.COMPLEX_TYPE)
			.find();
		return this;
	}

}
