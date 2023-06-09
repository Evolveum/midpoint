/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.selector.eval;

import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/** When evaluation subjected clauses, we have to know who the subject is. */
public interface SubjectedEvaluationContext {

    @Nullable String getPrincipalOid();

    @Nullable FocusType getPrincipalFocus();

    /**
     * Returns all OIDs that are considered to be "self": myself plus any delegators, according
     * to {@link DelegatorSelection} specified. Delegators are important e.g. when evaluating the "assignee" clause,
     * because the current principal can have the authority to see work items assigned also to the person that delegated
     * their case management privileges to them.
     */
    @NotNull Set<String> getSelfOids(@NotNull DelegatorSelection delegatorSelection);

    /**
     * Returns all OIDs that are considered to be "self", augmented with any abstract role membership information.
     * As for {@link #getSelfOids(DelegatorSelection)}, delegation may be involved here.
     */
    @NotNull Set<String> getSelfPlusRolesOids(@NotNull DelegatorSelection delegatorSelection);

    enum DelegatorSelection {

        /** We are not interested in any delegations. */
        NO_DELEGATOR,

        /** We are interested in delegations providing "case management" privileges. */
        CASE_MANAGEMENT,

        /** We are interested in delegations providing "access certification" privileges. */
        ACCESS_CERTIFICATION
    }
}
