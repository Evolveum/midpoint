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

import javax.xml.namespace.QName;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
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
}
