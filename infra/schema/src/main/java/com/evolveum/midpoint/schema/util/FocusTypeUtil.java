/**
 * Copyright (c) 2016-2017 Evolveum
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

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentSelectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrderConstraintsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ServiceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

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
	
	public static String determineConstructionResource(AssignmentType assignmentType) {
		ConstructionType construction = assignmentType.getConstruction();
		if (construction != null){
			if (construction.getResource() != null){
				return construction.getResource().getOid();
			} else if (construction.getResourceRef() != null){
				return construction.getResourceRef().getOid();
			} 
			
			return null;
		}
		
		return null;
	}
    
	public static String determineConstructionIntent(AssignmentType assignmentType) {
		ConstructionType construction = assignmentType.getConstruction();
		if (construction != null){
			if (construction.getIntent() != null){
				return construction.getIntent();
			} 
			
			return SchemaConstants.INTENT_DEFAULT;
		}
		
		throw new IllegalArgumentException("Construction not defined in the assigment.");
	}
	
	public static ShadowKindType determineConstructionKind(AssignmentType assignmentType) {
		ConstructionType construction = assignmentType.getConstruction();
		if (construction != null){
			if (construction.getKind() != null){
				return construction.getKind();
			} 
			
			return ShadowKindType.ACCOUNT;
		}
		
		throw new IllegalArgumentException("Construction not defined in the assigment.");
	}

	public static ProtectedStringType getPasswordValue(UserType user) {
		if (user == null) {
			return null;
		}
		CredentialsType creds = user.getCredentials();
		if (creds == null) {
			return null;
		}
		PasswordType passwd = creds.getPassword();
		if (passwd == null) {
			return null;
		}
		return passwd.getValue();
	}
	
	public static <O extends ObjectType> List<String> determineSubTypes(PrismObject<O> object) {
		if (object == null) {
			return null;
		}
		
		// TODO: get subType (from ObjectType)
		
		if (object.canRepresent(UserType.class)) {
			return (((UserType)object.asObjectable()).getEmployeeType());
		}
		if (object.canRepresent(OrgType.class)) {
			return (((OrgType)object.asObjectable()).getOrgType());
		}
		if (object.canRepresent(RoleType.class)) {
			List<String> roleTypes = new ArrayList<>(1);
			roleTypes.add((((RoleType)object.asObjectable()).getRoleType()));
			return roleTypes;
		}
		if (object.canRepresent(ServiceType.class)) {
			return (((ServiceType)object.asObjectable()).getServiceType());
		}
		return null;
	}

	public static <O extends ObjectType> boolean hasSubtype(PrismObject<O> object, String subtype) {
		List<String> objectSubtypes = determineSubTypes(object);
		if (objectSubtypes == null) {
			return false;
		}
		return objectSubtypes.contains(subtype);
	}

	public static <O extends ObjectType>  void setSubtype(PrismObject<O> object, List<String> subtypes) {
		
		// TODO: set subType (from ObjectType)
		
		List<String> objSubtypes = null;
		if (object.canRepresent(UserType.class)) {
			objSubtypes = (((UserType)object.asObjectable()).getEmployeeType());
		}
		if (object.canRepresent(OrgType.class)) {
			objSubtypes =  (((OrgType)object.asObjectable()).getOrgType());
		}
		if (object.canRepresent(RoleType.class)) {
			if (subtypes == null || subtypes.isEmpty()) {
				((RoleType)object.asObjectable()).setRoleType(null);
			} else {
				((RoleType)object.asObjectable()).setRoleType(subtypes.get(0));
			}
			return;
		}
		if (object.canRepresent(ServiceType.class)) {
			objSubtypes =  (((ServiceType)object.asObjectable()).getServiceType());
		}
		if (objSubtypes != null) {
			if (!objSubtypes.isEmpty()) {
				objSubtypes.clear();
			}
			if (subtypes != null) {
				objSubtypes.addAll(subtypes);
			}
		}

	}
}
