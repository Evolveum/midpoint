/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.selector.eval;

import com.evolveum.midpoint.schema.selector.spec.SelectorClause;

import org.jetbrains.annotations.NotNull;

public interface ClauseApplicabilityPredicate {

    boolean test(@NotNull SelectorClause clause, @NotNull ClauseMatchingContext ctx);
}
