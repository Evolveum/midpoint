/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api.util;

import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.QNameUtil;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationBehavioralDataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.BehaviorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import static com.evolveum.midpoint.util.MiscUtil.*;

/**
 * @author skublik
 */
public class AuthenticationEvaluatorUtil {

    /**
     * Checks whether focus has all the required abstract roles assigned (directly or indirectly - see MID-8123).
     */
    public static boolean checkRequiredAssignmentTargets(
            @NotNull FocusType focus, List<ObjectReferenceType> requiredTargetRefs) {
        if (requiredTargetRefs == null || requiredTargetRefs.isEmpty()) {
            return true;
        }
        for (ObjectReferenceType requiredTargetRef : requiredTargetRefs) {
            if (!containsRef(focus.getRoleMembershipRef(), requiredTargetRef)) {
                return false;
            }
        }
        return true;
    }

    /**
     * We don't use the built-in {@link Collection#contains(java.lang.Object)} because of its (a bit) unclear semantics
     * for references. We strictly want to match on OID, type (if present) and relation.
     */
    private static boolean containsRef(@NotNull List<ObjectReferenceType> references, ObjectReferenceType requiredTargetRef) {
        argCheck(requiredTargetRef != null, "Required assignment target value is null");
        String requiredOid =
                argNonNull(requiredTargetRef.getOid(), () -> "Required assignment target OID is null: " + requiredTargetRef);
        QName requiredType = requiredTargetRef.getType();
        QName requiredRelation = requiredTargetRef.getRelation();

        return references.stream()
                .anyMatch(
                        ref -> requiredOid.equals(ref.getOid())
                                && (requiredType == null || QNameUtil.match(requiredType, ref.getType()))
                                && PrismContext.get().relationMatches(requiredRelation, ref.getRelation()));
    }

    public static AuthenticationBehavioralDataType getBehavior(FocusType focus) {

        if (focus.getBehavior() == null){
            focus.setBehavior(new BehaviorType());
        }
        if (focus.getBehavior().getAuthentication() == null){
            focus.getBehavior().setAuthentication(new AuthenticationBehavioralDataType());
        }
        return focus.getBehavior().getAuthentication();
    }
}
