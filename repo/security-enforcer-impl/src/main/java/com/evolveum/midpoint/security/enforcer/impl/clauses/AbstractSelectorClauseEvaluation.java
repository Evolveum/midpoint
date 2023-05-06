/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.impl.clauses;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.security.enforcer.impl.SecurityEnforcerImpl;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Represents evaluation/processing of an object selector clause aimed either to determine applicability of the clause to
 * given object or to create a security filter related to given clause.
 *
 * See {@link com.evolveum.midpoint.security.enforcer.impl.ObjectSelectorEvaluation}
 * and {@link com.evolveum.midpoint.security.enforcer.impl.ObjectSelectorFilterEvaluation}.
 */
abstract class AbstractSelectorClauseEvaluation {

    /** Using {@link SecurityEnforcerImpl} to ensure log compatibility. */
    static final Trace LOGGER = TraceManager.getTrace(SecurityEnforcerImpl.class);

    @NotNull final ClauseEvaluationContext ctx;

    /** "Filter-processing" context. TODO implement more nicely */
    final ClauseFilterEvaluationContext fCtx;

    AbstractSelectorClauseEvaluation(@NotNull ClauseEvaluationContext ctx) {
        this.ctx = ctx;
        this.fCtx = ctx instanceof ClauseFilterEvaluationContext ? (ClauseFilterEvaluationContext) ctx : null;
    }
}
