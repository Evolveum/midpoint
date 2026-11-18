/*
 *
 *  Copyright (C) 2010- 2026 Evolveum and contributors
 *
 *   Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.schema.policy;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.Set;

/**
 * What the repository indexes for one policy rule, see {@link PolicyRuleIndexExtractor}.
 *
 * @param constraints element names of the constraints used anywhere in the rule (without `and`, `or`, `not`)
 * @param actions element names of the policy actions of the rule, empty if the rule has no actions
 * @param refs references of the rule that have an OID: constraint targets and marks
 */
public record PolicyRuleIndexData(
        @NotNull Set<QName> constraints,
        @NotNull Set<QName> actions,
        @NotNull Set<IndexedRef> refs) {
}


