/**
 * Copyright (c) 2016 Evolveum
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
package com.evolveum.midpoint.schema.util;

import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentSelectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrderConstraintsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

/**
 * @author semancik
 *
 */
public class FocusTypeUtil {
	
	public static AssignmentType createRoleAssignment(String roleOid) {
		return createTargetAssignment(roleOid, RoleType.COMPLEX_TYPE);
	}

	public static AssignmentType createOrgAssignment(String roleOid) {
		return createTargetAssignment(roleOid, OrgType.COMPLEX_TYPE);
	}
	
	public static AssignmentType createTargetAssignment(String targetOid, QName type) {
		AssignmentType assignmentType = new AssignmentType();
		ObjectReferenceType targetRef = new ObjectReferenceType();
		targetRef.setOid(targetOid);
		targetRef.setType(type);
		assignmentType.setTargetRef(targetRef);
		return assignmentType;
	}
	
	public static String dumpAssignment(AssignmentType assignmentType) { 
		StringBuilder sb = new StringBuilder();
		if (assignmentType.getConstruction() != null) {
			sb.append("Constr(").append(assignmentType.getConstruction().getDescription()).append(") ");
		}
		if (assignmentType.getTargetRef() != null) {
			sb.append("-[");
			if (assignmentType.getTargetRef().getRelation() != null) {
				sb.append(assignmentType.getTargetRef().getRelation().getLocalPart());
			}
			sb.append("]-> ").append(assignmentType.getTargetRef().getOid());
		}
		return sb.toString();
	}
	
	public static String dumpInducementConstraints(AssignmentType assignmentType) {
		if (assignmentType.getOrder() != null) {
			return assignmentType.getOrder().toString();
		}
		if (assignmentType.getOrderConstraint().isEmpty()) {
			return "1";
		}
		StringBuilder sb = new StringBuilder();
		for (OrderConstraintsType orderConstraint: assignmentType.getOrderConstraint()) {
			if (orderConstraint.getRelation() != null) {
				sb.append(orderConstraint.getRelation().getLocalPart());
			} else {
				sb.append("null");
			}
			sb.append(":");
			if (orderConstraint.getOrder() != null) {
				sb.append(orderConstraint.getOrder());
			} else {
				sb.append(orderConstraint.getOrderMin());
				sb.append("-");
				sb.append(orderConstraint.getOrderMax());
			}
			sb.append(",");
		}
		sb.setLength(sb.length() - 1);
		return sb.toString();
	}
	
	public static boolean selectorMatches(AssignmentSelectorType assignmentSelector, AssignmentType assignmentType) {
		if (assignmentType.getTargetRef() == null) {
			return false;
		}
		for (ObjectReferenceType selectorTargetRef: assignmentSelector.getTargetRef()) {
			if (MiscSchemaUtil.referenceMatches(selectorTargetRef, assignmentType.getTargetRef())) {
				return true;
			}
		}
		return false;
	}
}
