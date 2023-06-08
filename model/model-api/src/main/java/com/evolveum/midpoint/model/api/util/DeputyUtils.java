/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.util;

import com.evolveum.midpoint.model.api.context.AssignmentPath;
import com.evolveum.midpoint.model.api.context.AssignmentPathSegment;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.util.SchemaDeputyUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Experimental. TODO implement correctly
 */
public class DeputyUtils {

    @NotNull
    public static Collection<PrismReferenceValue> getDelegatorReferences(@NotNull FocusType focus,
            @NotNull RelationRegistry relationRegistry) {
        return focus.getDelegatedRef().stream()
                .filter(ref -> relationRegistry.isDelegation(ref.getRelation()))
                .map(ref -> ref.asReferenceValue().clone())
                .collect(Collectors.toList());
    }

    @NotNull
    public static Collection<String> getDelegatorOids(@NotNull FocusType focus, @NotNull RelationRegistry relationRegistry) {
        return getDelegatorReferences(focus, relationRegistry).stream()
                .map(PrismReferenceValue::getOid)
                .collect(Collectors.toList());
    }

    public static boolean isDelegationPresent(@NotNull FocusType deputy, @NotNull String delegatorOid,
            @NotNull RelationRegistry relationRegistry) {
        return getDelegatorOids(deputy, relationRegistry).contains(delegatorOid);
    }

    public static boolean isDelegationAssignment(AssignmentType assignment, @NotNull RelationRegistry relationRegistry) {
        return assignment != null
                && assignment.getTargetRef() != null
                && relationRegistry.isDelegation(assignment.getTargetRef().getRelation());
    }

    public static boolean isDelegationPath(@NotNull AssignmentPath assignmentPath, @NotNull RelationRegistry relationRegistry) {
        for (AssignmentPathSegment segment : assignmentPath.getSegments()) {
            if (!isDelegationAssignment(segment.getAssignment(), relationRegistry)) {
                return false;
            }
        }
        return true;
    }

    /** Deprecated "approval work items" item is not present in the returned list. */
    public static List<OtherPrivilegesLimitationType> extractLimitations(AssignmentPath assignmentPath) {
        List<OtherPrivilegesLimitationType> rv = new ArrayList<>();
        for (AssignmentPathSegment segment : assignmentPath.getSegments()) {
            OtherPrivilegesLimitationType limitation = segment.getAssignment().getLimitOtherPrivileges();
            if (limitation != null) {
                rv.add(migrate(limitation));
            }
        }
        return rv;
    }

    private static @NotNull OtherPrivilegesLimitationType migrate(@NotNull OtherPrivilegesLimitationType limitation) {
        WorkItemSelectorType legacy = limitation.getApprovalWorkItems();
        if (legacy == null) {
            return limitation; // nothing to migrate
        }
        OtherPrivilegesLimitationType migrated = limitation.clone();
        if (migrated.getCaseManagementWorkItems() == null) {
            migrated.setCaseManagementWorkItems(legacy.clone());
        }
        migrated.setApprovalWorkItems(null);
        return migrated;
    }

    public static boolean limitationsAllow(
            List<OtherPrivilegesLimitationType> limitations, QName itemName, AbstractWorkItemType workItem) {
        // temporary solution; we do not use work items selectors yet
        return SchemaDeputyUtil.limitationsAllow(limitations, itemName);
    }
}
