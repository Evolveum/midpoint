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

import javax.xml.namespace.QName;

import org.testng.AssertJUnit;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author semancik
 *
 */
public class AssignmentFinder<F extends FocusType, FA extends FocusAsserter<F, RA>,RA> {

	private final AssignmentsAsserter<F,FA,RA> assignmentsAsserter;
	private String targetOid;
	private QName targetType;
	
	public AssignmentFinder(AssignmentsAsserter<F,FA,RA> assignmentsAsserter) {
		this.assignmentsAsserter = assignmentsAsserter;
	}
	
	public AssignmentFinder<F,FA,RA> targetOid(String targetOid) {
		this.targetOid = targetOid;
		return this;
	}
	
	public AssignmentFinder<F,FA,RA> targetType(QName targetType) {
		this.targetType = targetType;
		return this;
	}

	public AssignmentAsserter<AssignmentsAsserter<F, FA, RA>> find() throws ObjectNotFoundException, SchemaException {
		AssignmentType found = null;
		PrismObject<?> foundTarget = null;
		for (AssignmentType assignment: assignmentsAsserter.getAssignments()) {
			PrismObject<ShadowType> assignmentTarget = null;
//			PrismObject<ShadowType> assignmentTarget = assignmentsAsserter.getTarget(assignment.getOid());
			if (matches(assignment, assignmentTarget)) {
				if (found == null) {
					found = assignment;
					foundTarget = assignmentTarget;
				} else {
					fail("Found more than one link that matches search criteria");
				}
			}
		}
		if (found == null) {
			fail("Found no link that matches search criteria");
		}
		return assignmentsAsserter.forAssignment(found, foundTarget);
	}
	
	public AssignmentsAsserter<F,FA,RA> assertNone() throws ObjectNotFoundException, SchemaException {
		for (AssignmentType assignment: assignmentsAsserter.getAssignments()) {
			PrismObject<ShadowType> assignmentTarget = null;
//			PrismObject<ShadowType> assignmentTarget = assignmentsAsserter.getTarget(assignment.getOid());
			if (matches(assignment, assignmentTarget)) {
				fail("Found assignment target while not expecting it: "+formatTarget(assignment, assignmentTarget));
			}
		}
		return assignmentsAsserter;
	}
	
	public AssignmentsAsserter<F,FA,RA> assertAll() throws ObjectNotFoundException, SchemaException {
		for (AssignmentType assignment: assignmentsAsserter.getAssignments()) {
			PrismObject<ShadowType> assignmentTarget = null;
//			PrismObject<ShadowType> assignmentTarget = assignmentsAsserter.getTarget(assignment.getOid());
			if (!matches(assignment, assignmentTarget)) {
				fail("Found assignment that does not match search criteria: "+formatTarget(assignment, assignmentTarget));
			}
		}
		return assignmentsAsserter;
	}
	
	private String formatTarget(AssignmentType assignment, PrismObject<ShadowType> assignmentTarget) {
		if (assignmentTarget != null) {
			return assignmentTarget.toString();
		}
		return assignment.getTargetRef().toString();
	}

	private boolean matches(AssignmentType assignment, PrismObject<?> targetObject) throws ObjectNotFoundException, SchemaException {
		ObjectReferenceType targetRef = assignment.getTargetRef();
		ObjectType targetObjectType = null;
		if (targetObject != null) {
			targetObjectType = (ObjectType) targetObject.asObjectable();
		}
		
		if (targetOid != null) {
			if (targetRef == null || !targetOid.equals(targetRef.getOid())) {
				return false;
			}
		}
		
		if (targetType != null) {
			if (targetRef == null || !QNameUtil.match(targetType, targetRef.getType())) {
				return false;
			}
		}		
		
		// TODO: more criteria
		return true;
	}

	protected void fail(String message) {
		AssertJUnit.fail(message);
	}

}
