/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.cases;

import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility methods related to case management. (Not directly tied to any of the particular prism objects or containers.)
 */
public class CaseRelatedUtils {

    // TODO rethink interface of this method
    // returns parent-less values
    public static void computeAssignees(
            List<ObjectReferenceType> newAssignees,
            List<ObjectReferenceType> delegatedTo,
            List<ObjectReferenceType> delegates,
            WorkItemDelegationMethodType method,
            List<ObjectReferenceType> currentAssignees) {
        newAssignees.clear();
        delegatedTo.clear();
        switch (method) {
            case ADD_ASSIGNEES: newAssignees.addAll(CloneUtil.cloneCollectionMembers(currentAssignees)); break;
            case REPLACE_ASSIGNEES: break;
            default: throw new UnsupportedOperationException("Delegation method " + method + " is not supported yet.");
        }
        for (ObjectReferenceType delegate : delegates) {
            if (delegate.getType() != null && !QNameUtil.match(UserType.COMPLEX_TYPE, delegate.getType())) {
                throw new IllegalArgumentException("Couldn't use non-user object as a delegate: " + delegate);
            }
            if (delegate.getOid() == null) {
                throw new IllegalArgumentException("Couldn't use no-OID reference as a delegate: " + delegate);
            }
            if (!ObjectTypeUtil.containsOid(newAssignees, delegate.getOid())) {
                newAssignees.add(delegate.clone());
                delegatedTo.add(delegate.clone());
            }
        }
    }

    public static @NotNull List<CaseWorkItemType> getOpenWorkItems(@NotNull CaseType aCase) {
        return aCase.getWorkItem().stream()
                .filter(wi -> wi.getCloseTimestamp() == null)
                .collect(Collectors.toList());
    }
}
