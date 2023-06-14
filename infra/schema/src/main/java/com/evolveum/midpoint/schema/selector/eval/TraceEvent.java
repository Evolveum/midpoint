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

import com.evolveum.midpoint.util.DebugUtil;

import com.evolveum.midpoint.util.MiscUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Something of interest during tracing selectors and their clauses processing (matching and filter evaluation).
 *
 * An abstract representation that will eventually get converted into a logfile entry, or CSV line, trace file item, and so on.
 */
public abstract class TraceEvent {

    final @NotNull SelectorProcessingContext ctx;
    final @Nullable String message;
    final @Nullable Object[] arguments;

    TraceEvent(
            @NotNull SelectorProcessingContext ctx, @Nullable String message, @Nullable Object[] arguments) {
        this.ctx = ctx;
        this.message = message;
        this.arguments = arguments;
    }

    public @NotNull ClauseProcessingContextDescription getDescription() {
        return ctx.description;
    }

    public abstract @NotNull TraceRecord defaultTraceRecord();

    /** Just a marker. */
    public interface SelectorProcessingStarted { }

    /** Just a marker. */
    public interface SelectorProcessingFinished { }

    static abstract class SelectorRelated extends TraceEvent {

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

    static class MatchingStarted extends SelectorRelated implements SelectorProcessingStarted {

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
            return new TraceRecord(
                    String.format("determining whether %s does match '%s'",
                            selector.getHumanReadableDesc(), MiscUtil.getDiagInfo(value)));
        }
    }

    static class MatchingFinished extends SelectorRelated implements SelectorProcessingFinished {

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
            return new TraceRecord(
                    String.format(
                            "%s %s '%s'", selector.getHumanReadableDesc(),
                            matches ? "matches" : "DOES NOT match",
                            MiscUtil.getDiagInfo(value)));
        }
    }

    static abstract class ClauseRelated extends TraceEvent {
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

    static class FilterProcessingStarted extends SelectorRelated implements SelectorProcessingStarted {

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

    static class FilterProcessingFinished extends SelectorRelated implements SelectorProcessingFinished {

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
                return new TraceRecord(
                        String.format("finished deriving filter in %s (applicable)", ctx.description.getText()),
                        DebugUtil.debugDump(fCtx.filterCollector.getFilter(), 1));
            } else {
                return new TraceRecord(
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
            String suffix;
            if (message != null) {
                suffix = ": " + String.format(message, arguments);
            } else {
                suffix = "";
            }
            return new TraceRecord(
                    String.format("clause '%s' %sapplicable%s",
                            clause.getName(), applicable ? "" : "NOT ", suffix));
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
            String messageFragment;
            if (message != null) {
                messageFragment = " [" + String.format(message, arguments) + "]";
            } else {
                messageFragment = "";
            }
            return new TraceRecord(
                    String.format("clause '%s' provided filter conjunct%s: %s",
                            clause.getName(), messageFragment, conjunct));
        }
    }
}
