/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.selector.eval;

import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.selector.spec.SelectorClause;

import com.evolveum.midpoint.schema.selector.spec.ValueSelector;

import com.evolveum.midpoint.schema.traces.details.AbstractTraceEvent;
import com.evolveum.midpoint.schema.traces.details.TraceRecord;
import com.evolveum.midpoint.util.DebugUtil;

import com.evolveum.midpoint.util.MiscUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Something of interest during tracing selectors and their clauses processing (matching and filter evaluation).
 */
public abstract class SelectorTraceEvent extends AbstractTraceEvent {

    final @NotNull SelectorProcessingContext ctx;

    SelectorTraceEvent(
            @NotNull SelectorProcessingContext ctx, @Nullable String message, @Nullable Object[] arguments) {
        super(message, arguments);
        this.ctx = ctx;
    }

    public @NotNull String getId() {
        return ctx.description.getId();
    }

    /** Just a marker. */
    public interface Start { }

    /** Just a marker. */
    public interface End { }

    static abstract class SelectorRelated extends SelectorTraceEvent {

        @NotNull final ValueSelector selector;

        SelectorRelated(
                @NotNull ValueSelector selector,
                @NotNull SelectorProcessingContext ctx,
                @Nullable String message,
                @Nullable Object... arguments) {
            super(ctx, message, arguments);
            this.selector = selector;
        }
    }

    static class MatchingStarted extends SelectorRelated implements Start {

        @NotNull private final PrismValue value;

        MatchingStarted(
                @NotNull ValueSelector selector,
                @NotNull PrismValue value,
                @NotNull SelectorProcessingContext ctx) {
            super(selector, ctx, null);
            this.value = value;
        }

        @Override
        @NotNull
        public TraceRecord defaultTraceRecord() {
            return TraceRecord.of(
                    String.format("determining whether %s does match '%s'",
                            selector.getHumanReadableDesc(), MiscUtil.getDiagInfo(value)));
        }
    }

    static class MatchingFinished extends SelectorRelated implements End {

        @NotNull private final PrismValue value;
        private final boolean matches;

        MatchingFinished(
                @NotNull ValueSelector selector,
                @NotNull PrismValue value,
                boolean matches,
                @NotNull SelectorProcessingContext ctx) {
            super(selector, ctx, null);
            this.value = value;
            this.matches = matches;
        }

        @Override
        @NotNull
        public TraceRecord defaultTraceRecord() {
            return TraceRecord.of(
                    String.format(
                            "%s %s '%s'", selector.getHumanReadableDesc(),
                            matches ? "matches" : "DOES NOT match",
                            MiscUtil.getDiagInfo(value)));
        }
    }

    static abstract class ClauseRelated extends SelectorTraceEvent {
        @NotNull final SelectorClause clause;

        ClauseRelated(
                @NotNull SelectorClause clause,
                @Nullable String message,
                @Nullable Object[] arguments,
                @NotNull SelectorProcessingContext ctx) {
            super(ctx, message, arguments);
            this.clause = clause;
        }
    }

    static class FilterProcessingStarted extends SelectorRelated implements Start {

        FilterProcessingStarted(
                @NotNull ValueSelector selector,
                @NotNull SelectorProcessingContext ctx) {
            super(selector, ctx, null);
        }

        @Override
        @NotNull
        public TraceRecord defaultTraceRecord() {
            return new TraceRecord(
                    String.format("starting deriving filter in %s", ctx.description.getText()),
                    selector.debugDump(1));
        }
    }

    static class FilterProcessingFinished extends SelectorRelated implements End {

        private final FilteringContext fCtx;
        private final boolean applicable;

        FilterProcessingFinished(
                @NotNull ValueSelector selector,
                boolean applicable,
                @NotNull FilteringContext ctx) {
            super(selector, ctx, null);
            this.fCtx = ctx;
            this.applicable = applicable;
        }

        @Override
        @NotNull
        public TraceRecord defaultTraceRecord() {
            if (applicable) {
                return TraceRecord.of(
                        String.format("finished deriving filter in %s (applicable)", ctx.description.getText()),
                        DebugUtil.debugDump(fCtx.filterCollector.getFilter(), 1));
            } else {
                return TraceRecord.of(
                        String.format("finished deriving filter in %s: NOT applicable", ctx.description.getText()));
            }
        }
    }

    static class ClauseApplicability extends ClauseRelated {

        private final boolean applicable;

        ClauseApplicability(
                @NotNull SelectorClause clause,
                boolean applicable,
                @NotNull SelectorProcessingContext ctx,
                @Nullable String message,
                @Nullable Object[] arguments) {
            super(clause, message, arguments, ctx);
            this.applicable = applicable;
        }

        @Override
        @NotNull
        public TraceRecord defaultTraceRecord() {
            return TraceRecord.of(
                    String.format("clause '%s' %sapplicable%s",
                            clause.getName(),
                            applicable ? "" : "NOT ",
                            getFormattedMessage(": ", "")));
        }
    }

    static class ConjunctAdded extends ClauseRelated {

        private final ObjectFilter conjunct;

        ConjunctAdded(
                @NotNull SelectorClause clause,
                ObjectFilter conjunct,
                @Nullable String message,
                @Nullable Object[] arguments,
                @NotNull FilteringContext ctx) {
            super(clause, message, arguments, ctx);
            this.conjunct = conjunct;
        }

        @Override
        @NotNull
        public TraceRecord defaultTraceRecord() {
            return TraceRecord.of(
                    String.format("clause '%s' provided filter conjunct%s: %s",
                            clause.getName(), getFormattedMessage(" [", "]"), conjunct));
        }
    }
}
