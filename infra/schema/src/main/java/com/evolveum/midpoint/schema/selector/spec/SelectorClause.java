/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.selector.spec;

import java.io.Serializable;
import java.util.Collection;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.selector.eval.ClauseFilteringContext;
import com.evolveum.midpoint.schema.selector.eval.ClauseMatchingContext;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;

/**
 * A clause of a {@link ValueSelector}.
 *
 * Immutable.
 */
public abstract class SelectorClause implements DebugDumpable, Serializable {

    public abstract boolean matches(
            @NotNull PrismValue value,
            @NotNull ClauseMatchingContext ctx)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException;

    public abstract boolean applyFilter(@NotNull ClauseFilteringContext ctx)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException ;

    void traceNotApplicable(ClauseMatchingContext ctx, String message, Object... arguments) {
        ctx.traceClauseNotApplicable(this, message, arguments);
    }

    void traceApplicable(ClauseMatchingContext ctx, String message, Object... arguments) {
        ctx.traceClauseApplicable(this, message, arguments);
    }

    void traceApplicability(ClauseMatchingContext ctx, boolean matches, String message, Object... arguments) {
        if (matches) {
            traceApplicable(ctx, message, arguments);
        } else {
            traceNotApplicable(ctx, message, arguments);
        }
    }

    /** Human-understandable name to be used e.g. in tracing messages. */
    abstract public @NotNull String getName();

    void addConjunct(ClauseFilteringContext ctx, ObjectFilter objectFilter) {
        ctx.addConjunct(this, objectFilter);
    }

    void addConjunct(ClauseFilteringContext ctx, ObjectFilter objectFilter, String message, Object... arguments) {
        ctx.addConjunct(this, objectFilter, message, arguments);
    }

    @Override
    public String debugDump(int indent) {
        var sb = DebugUtil.createTitleStringBuilder(getName(), indent);
        addDebugDumpContent(sb, indent);
        return sb.toString();
    }

    abstract void addDebugDumpContent(StringBuilder sb, int indent);

    public static <T extends SelectorClause> @Nullable T getSingle(@NotNull Collection<SelectorClause> clauses, Class<T> type) {
        //noinspection unchecked
        return MiscUtil.extractSingleton(clauses.stream()
                .filter(c -> type.isAssignableFrom(c.getClass()))
                .map(c -> (T) c)
                .collect(Collectors.toList()));
    }

}
