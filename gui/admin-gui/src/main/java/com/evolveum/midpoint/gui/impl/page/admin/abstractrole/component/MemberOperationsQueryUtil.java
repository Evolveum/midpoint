/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.abstractrole.component;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.S_FilterEntry;
import com.evolveum.midpoint.prism.query.builder.S_FilterExit;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.util.MiscUtil.argCheck;

/** Helps with creating queries regarding members of an abstract role. */
public class MemberOperationsQueryUtil {

    private static final Trace LOGGER = TraceManager.getTrace(MemberOperationsQueryUtil.class);

    /**
     * Creates a query covering all direct (assigned) members of an abstract role.
     *
     * @param targetObject The role.
     * @param memberType Type of members to be looked for.
     * @param relations Relations (of member->target assignment) to be looked for.
     * Should not be empty (although it is not guaranteed now).
     * @param tenant Tenant to be looked for (assignment/tenantRef)
     * @param project Org to be looked for (assignment/orgRef)
     */
    static @NotNull ObjectQuery createDirectMemberQuery(
            AbstractRoleType targetObject,
            @NotNull QName memberType,
            Collection<QName> relations,
            ObjectReferenceType tenant,
            ObjectReferenceType project) {
        // We assume tenantRef.relation and orgRef.relation are always default ones (see also MID-3581)
        S_FilterEntry q0 = PrismContext.get().queryFor(AssignmentHolderType.class);
        if (!AssignmentHolderType.COMPLEX_TYPE.equals(memberType)) {
            q0 = q0.type(memberType);
        }

        // Use exists filter to build a query like this:
        // $a/targetRef = oid1 and $a/tenantRef = oid2 and $a/orgRef = oid3
        S_FilterExit q = q0.exists(AssignmentHolderType.F_ASSIGNMENT)
                .block()
                .item(AssignmentType.F_TARGET_REF)
                .ref(createReferenceValuesList(targetObject, relations));

        if (tenant != null && StringUtils.isNotEmpty(tenant.getOid())) {
            q = q.and().item(AssignmentType.F_TENANT_REF).ref(tenant.getOid());
        }

        if (project != null && StringUtils.isNotEmpty(project.getOid())) {
            q = q.and().item(AssignmentType.F_ORG_REF).ref(project.getOid());
        }

        ObjectQuery query = q.endBlock().build();
        LOGGER.trace("Searching members of role {} with query:\n{}", targetObject.getOid(), query.debugDumpLazily());
        return query;
    }

    /**
     * Creates reference values pointing to given target with given relations.
     *
     * @param relations The relations. Must be at least one, otherwise the resulting list (to be used in a query, presumably)
     * will be empty, making the query wrong.
     */
    public static @NotNull List<PrismReferenceValue> createReferenceValuesList(@NotNull AbstractRoleType targetObject,
            @NotNull Collection<QName> relations) {
        argCheck(!relations.isEmpty(), "At least one relation must be specified");
        return relations.stream()
                .map(relation -> ObjectTypeUtil.createObjectRef(targetObject, relation).asReferenceValue())
                .collect(Collectors.toList());
    }

    /**
     * Creates reference values pointing to given target with given relations.
     *
     * @param relations The relations. Must be at least one, otherwise the resulting list (to be used in a query, presumably)
     * will be empty, making the query wrong.
     */
    public static @NotNull List<PrismReferenceValue> createReferenceValuesList(
            @NotNull ObjectReferenceType targetObjectRef, @NotNull Collection<QName> relations) {
        argCheck(!relations.isEmpty(), "At least one relation must be specified");
        return relations.stream()
                .map(relation -> targetObjectRef.clone().relation(relation).asReferenceValue())
                .collect(Collectors.toList());
    }

    /**
     * Creates a query covering all selected objects (converts list of objects to a multivalued "OID" query).
     */
    static @NotNull ObjectQuery createSelectedObjectsQuery(@NotNull List<? extends ObjectType> selectedObjects) {
        Set<String> oids = new HashSet<>(ObjectTypeUtil.getOids(selectedObjects));
        return PrismContext.get().queryFor(AssignmentHolderType.class)
                .id(oids.toArray(new String[0]))
                .build();
    }
}
