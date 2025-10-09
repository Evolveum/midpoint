/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.selector.eval;

import com.evolveum.midpoint.schema.selector.spec.SelectorClause;

import org.jetbrains.annotations.NotNull;

/** Externally-imposed exception from application of some clauses. */
public interface ClauseApplicabilityPredicate {

    boolean test(@NotNull SelectorClause clause, @NotNull SelectorProcessingContext ctx);
}
