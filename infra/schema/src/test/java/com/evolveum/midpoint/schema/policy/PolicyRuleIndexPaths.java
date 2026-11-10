/*
 *
 *  Copyright (C) 2010- 2026 Evolveum and contributors
 *
 *   Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.schema.policy;

import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyRuleType;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

public class PolicyRuleIndexPaths {

    public static final String MARK_REF = PolicyRuleType.F_MARK_REF.getLocalPart();   // "markRef"

   private PolicyRuleIndexPaths(){}

    /** E.g. (exclusion, targetRef) -> "exclusion/targetRef". */
    public static @NotNull String of(@NotNull QName constraintName, @NotNull QName refItem) {
        return constraintName.getLocalPart() + "/" + refItem.getLocalPart();
    }
}
