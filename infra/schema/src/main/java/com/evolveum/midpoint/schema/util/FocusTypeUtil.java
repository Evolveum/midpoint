/*
 * Copyright (c) 2016-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.util;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismContext;
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
import org.jetbrains.annotations.NotNull;

import static java.util.Collections.emptyList;

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

    public static boolean selectorMatches(AssignmentSelectorType assignmentSelector, AssignmentType assignmentType,
            PrismContext prismContext) {
        if (assignmentType.getTargetRef() == null) {
            return false;
        }
        for (ObjectReferenceType selectorTargetRef: assignmentSelector.getTargetRef()) {
            if (MiscSchemaUtil.referenceMatches(selectorTargetRef, assignmentType.getTargetRef(), prismContext)) {
                return true;
            }
        }
        return false;
    }

    public static String determineConstructionResource(AssignmentType assignmentType) {
        ConstructionType construction = assignmentType.getConstruction();
        if (construction != null){
            if (construction.getResourceRef() != null){
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

    @NotNull    // to eliminate the need for extensive NPE avoidance
    public static <O extends ObjectType> List<String> determineSubTypes(O object) {
        return object != null ? determineSubTypes(object.asPrismObject()) : emptyList();
    }

    @NotNull    // to eliminate the need for extensive NPE avoidance
    public static <O extends ObjectType> List<String> determineSubTypes(PrismObject<O> object) {
        if (object == null) {
            return emptyList();
        }

        List<String> subtypes = object.asObjectable().getSubtype();
        if (!subtypes.isEmpty()) {
            return subtypes;
        }

        if (object.canRepresent(UserType.class)) {
            return (((UserType)object.asObjectable()).getEmployeeType());
        }
        if (object.canRepresent(OrgType.class)) {
            return (((OrgType)object.asObjectable()).getOrgType());
        }
        if (object.canRepresent(RoleType.class)) {
            // TODO why not return simply .getRoleType() [pmed]
            List<String> roleTypes = new ArrayList<>(1);
            roleTypes.add((((RoleType)object.asObjectable()).getRoleType()));
            return roleTypes;
        }
        if (object.canRepresent(ServiceType.class)) {
            return (((ServiceType)object.asObjectable()).getServiceType());
        }
        return emptyList();
    }

    public static <O extends ObjectType> boolean hasSubtype(PrismObject<O> object, String subtype) {
        List<String> objectSubtypes = determineSubTypes(object);
        if (objectSubtypes == null) {
            return false;
        }
        return objectSubtypes.contains(subtype);
    }

    public static <O extends ObjectType>  void setSubtype(PrismObject<O> object, List<String> subtypes) {

        List<String> objSubtypes = object.asObjectable().getSubtype();
        if (!objSubtypes.isEmpty()) {
            objSubtypes.clear();
        }
        if (subtypes != null) {
            objSubtypes.addAll(subtypes);
        }
    }
}
