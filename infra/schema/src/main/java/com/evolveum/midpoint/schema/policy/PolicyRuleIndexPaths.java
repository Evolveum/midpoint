/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.policy;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyRuleType;

/**
 * Canonical paths of the references indexed for a policy rule, relative to the rule itself.
 *
 * The repository stores the path in its URI cache and the policy rule reference tables refer to it (`pathId`).
 * Both the indexer (write) and the query mapping (search) take the path from here, so they always match.
 *
 * Format: local names only, separated by `/`, e.g. `markRef` or `exclusion/targetRef`.
 * Nesting of the constraint in `and`, `or`, `not` or `transition` is not part of the path.
 */
public final class PolicyRuleIndexPaths {

    /** Path of {@link PolicyRuleType#F_MARK_REF}. */
    public static final String MARK_REF = PolicyRuleType.F_MARK_REF.getLocalPart();

    private PolicyRuleIndexPaths() {
    }

    /** E.g. `(exclusion, targetRef)` gives `exclusion/targetRef`. */
    public static @NotNull String of(@NotNull QName constraintName, @NotNull QName refItemName) {
        return constraintName.getLocalPart() + "/" + refItemName.getLocalPart();
    }
}
