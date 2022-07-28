/*
 * Copyright (c) 2016-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.util;

import java.util.List;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static java.util.Collections.emptyList;

/**
 * @author semancik
 *
 */
public class FocusTypeUtil {

    public static AssignmentType createRoleAssignment(String roleOid) {
        return createTargetAssignment(roleOid, RoleType.COMPLEX_TYPE);
    }

    public static AssignmentType createOrgAssignment(String orgOid) {
        return createTargetAssignment(orgOid, OrgType.COMPLEX_TYPE);
    }

    public static AssignmentType createArchetypeAssignment(String archetypeOid) {
        return createTargetAssignment(archetypeOid, ArchetypeType.COMPLEX_TYPE);
    }

    public static <AH extends AssignmentHolderType> void addArchetypeAssignments(PrismObject<AH> object, List<ObjectReferenceType> archetypeRefs) {
        List<AssignmentType> archetypeAssignments = archetypeRefs.stream()
                .map(archetypeRef -> createTargetAssignment(archetypeRef))
                .collect(Collectors.toList());
        object.asObjectable().getAssignment().addAll(archetypeAssignments);
    }

    public static AssignmentType createTargetAssignment(String targetOid, QName type) {
        ObjectReferenceType targetRef = new ObjectReferenceType();
        targetRef.setOid(targetOid);
        targetRef.setType(type);
        return createTargetAssignment(targetRef);
    }

    public static AssignmentType createTargetAssignment(ObjectReferenceType targetRef) {
        AssignmentType assignmentType = new AssignmentType();
        assignmentType.setTargetRef(targetRef);
        return assignmentType;
    }

    public static String dumpAssignment(AssignmentType assignment) {
        StringBuilder sb = new StringBuilder();
        if (assignment.getConstruction() != null) {
            sb.append("Constr(").append(assignment.getConstruction().getDescription()).append(") ");
        }
        if (assignment.getTargetRef() != null) {
            sb.append("-[");
            if (assignment.getTargetRef().getRelation() != null) {
                sb.append(assignment.getTargetRef().getRelation().getLocalPart());
            }
            sb.append("]-> ").append(assignment.getTargetRef().getOid());
        }
        return sb.toString();
    }

    public static Object dumpAssignmentLazily(AssignmentType assignment) {
        return new Object() {
            @Override
            public String toString() {
                return dumpAssignment(assignment);
            }
        };
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

    public static @NotNull List<String> determineSubTypes(ObjectType object) {
        return object != null ? determineSubTypes(object.asPrismObject()) : emptyList();
    }

    public static @NotNull List<String> determineSubTypes(PrismObject<? extends ObjectType> object) {
        return object != null ? object.asObjectable().getSubtype() : emptyList();
    }

    public static <O extends ObjectType> boolean hasSubtype(PrismObject<O> object, String subtype) {
        return determineSubTypes(object)
                .contains(subtype);
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

    @NotNull
    public static <F extends FocusType> List<ObjectReferenceType> getLiveLinkRefs(F focus) {
        RelationRegistry relationRegistry = SchemaService.get().relationRegistry();
        return focus.getLinkRef().stream()
                .filter(ref -> relationRegistry.isMember(ref.getRelation()))
                .collect(Collectors.toList());
    }

    public static @Nullable FocusIdentityType getMatchingIdentity(
            @NotNull FocusType focus, @Nullable FocusIdentitySourceType source) {
        FocusIdentitiesType identities = focus.getIdentities();
        return identities != null ? FocusIdentitiesTypeUtil.getMatchingIdentity(identities, source) : null;
    }

    public static void addOrReplaceIdentity(@NotNull FocusType focus, @NotNull FocusIdentityType identity) {
        deleteCompatibleIdentity(focus, identity);
        addIdentity(focus, identity);
    }

    private static void addIdentity(@NotNull FocusType focus, @NotNull FocusIdentityType identity) {
        FocusIdentitiesType identities = focus.getIdentities();
        if (identities == null) {
            focus.setIdentities(identities = new FocusIdentitiesType());
        }
        identities.getIdentity().add(identity);
    }

    private static void deleteCompatibleIdentity(@NotNull FocusType focus, @NotNull FocusIdentityType identity) {
        FocusIdentitiesType identities = focus.getIdentities();
        if (identities != null) {
            identities.getIdentity().removeIf(
                    i -> FocusIdentityTypeUtil.matches(i, identity));
        }
    }
}
