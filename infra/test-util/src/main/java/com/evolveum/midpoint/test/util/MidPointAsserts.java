/**
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */
package com.evolveum.midpoint.test.util;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import javax.xml.namespace.QName;

import org.testng.AssertJUnit;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

/**
 * @author semancik
 *
 */
public class MidPointAsserts {
	
	public static void assertAssigned(PrismObject<UserType> user, String targetOid, QName refType) {
		UserType userType = user.asObjectable();
		for (AssignmentType assignmentType: userType.getAssignment()) {
			ObjectReferenceType targetRef = assignmentType.getTargetRef();
			if (targetRef != null) {
				if (refType.equals(targetRef.getType())) {
					if (targetOid.equals(targetRef.getOid())) {
						return;
					}
				}
			}
		}
		AssertJUnit.fail(user + " does not have assigned "+refType.getLocalPart()+" "+targetOid);
	}
	
	public static void assertNotAssigned(PrismObject<UserType> user, String targetOid, QName refType) {
		UserType userType = user.asObjectable();
		for (AssignmentType assignmentType: userType.getAssignment()) {
			ObjectReferenceType targetRef = assignmentType.getTargetRef();
			if (targetRef != null) {
				if (refType.equals(targetRef.getType())) {
					if (targetOid.equals(targetRef.getOid())) {
						AssertJUnit.fail(user + " does have assigned "+refType.getLocalPart()+" "+targetOid+" while not expecting it");
					}
				}
			}
		}
	}
	
	public static void assertAssignments(PrismObject<UserType> user, int expectedNumber) {
		UserType userType = user.asObjectable();
		assertEquals("Unexepected number of assignments in "+user+": "+userType.getAssignment(), expectedNumber, userType.getAssignment().size());
	}
	
	public static void assertAssignedRole(PrismObject<UserType> user, String roleOid) {
		assertAssigned(user, roleOid, RoleType.COMPLEX_TYPE);
	}
	
	public static void assertNotAssignedRole(PrismObject<UserType> user, String roleOid) {
		assertNotAssigned(user, roleOid, RoleType.COMPLEX_TYPE);
	}
	
	public static void assertAssignedOrg(PrismObject<UserType> user, String orgOid) {
		assertAssigned(user, orgOid, OrgType.COMPLEX_TYPE);
	}
	
	public static void assertHasOrg(PrismObject<UserType> user, String orgOid) {
		for (ObjectReferenceType orgRef: user.asObjectable().getParentOrgRef()) {
			if (orgOid.equals(orgRef.getOid())) {
				return;
			}
		}
		AssertJUnit.fail(user + " does not have org " + orgOid);
	}
	
	public static void assertHasNoOrg(PrismObject<UserType> user) {
		assertTrue(user + " does have orgs "+user.asObjectable().getParentOrgRef()+" while not expecting them", user.asObjectable().getParentOrgRef().isEmpty());
	}

	public static void assertHasOrgs(PrismObject<UserType> user, int expectedNumber) {
		UserType userType = user.asObjectable();
		assertEquals("Unexepected number of orgs in "+user+": "+userType.getParentOrgRef(), expectedNumber, userType.getParentOrgRef().size());
	}

}
