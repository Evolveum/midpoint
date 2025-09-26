/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.impl;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectValue;
import com.evolveum.midpoint.schema.AccessDecision;
import com.evolveum.midpoint.security.enforcer.impl.prism.UpdatablePrismEntityOpConstraints;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.schema.traces.details.AbstractTraceEvent;
import com.evolveum.midpoint.schema.traces.details.TraceRecord;
import com.evolveum.midpoint.util.DebugUtil;

import java.util.List;

import static com.evolveum.midpoint.security.enforcer.impl.SecurityEnforcerImpl.prettyActionUrl;

public abstract class SecurityTraceEvent extends AbstractTraceEvent {

    SecurityTraceEvent(@Nullable String message, @Nullable Object[] arguments) {
        super(message, arguments);
    }

    /** A marker interface. */
    interface Start { }

    /** A marker interface. */
    interface End { }

    static abstract class OperationRelated<OP extends EnforcerOperation> extends SecurityTraceEvent {
        @NotNull final OP operation;

        OperationRelated(@NotNull OP operation, @Nullable String message, @Nullable Object[] arguments) {
            super(message, arguments);
            this.operation = operation;
        }
    }

    static class FilterOperationStarted extends OperationRelated<EnforcerFilterOperation<?, ?>> implements Start {

        FilterOperationStarted(@NotNull EnforcerFilterOperation<?, ?> operation) {
            super(operation, null, null);
        }

        @Override
        public @NotNull TraceRecord defaultTraceRecord() {
            return TraceRecord.of(
                    "Computing security filter for principal=%s, searchResultType=%s, searchType=%s, orig filter=%s".formatted(
                            operation.username,
                            TracingUtil.getTypeName(operation.filterType),
                            operation.selectorExtractor,
                            operation.origFilter));
        }
    }

    static class FilterOperationFinished<F> extends OperationRelated<EnforcerFilterOperation<?, F>> implements End {

        private final F resultingFilter;

        FilterOperationFinished(@NotNull EnforcerFilterOperation<?, F> operation, F resultingFilter) {
            super(operation, null, null);
            this.resultingFilter = resultingFilter;
        }

        @Override
        public @NotNull TraceRecord defaultTraceRecord() {
            return TraceRecord.of(
                    "Computed security filter for principal=%s, searchResultType=%s: %s".formatted(
                            operation.username,
                            TracingUtil.getTypeName(operation.filterType),
                            resultingFilter),
                    DebugUtil.debugDump(resultingFilter, 1));
        }
    }

    static abstract class PartialFilterOperationRelated<F> extends OperationRelated<EnforcerFilterOperation<?, F>> {

        @NotNull private final EnforcerFilterOperation<?, F>.PartialOp partialOp;

        PartialFilterOperationRelated(
                @NotNull EnforcerFilterOperation<?, F>.PartialOp partialOp,
                @Nullable String message,
                @Nullable Object[] arguments) {
            super(partialOp.getEnforcerFilterOperation(), message, arguments);
            this.partialOp = partialOp;
        }

        public @NotNull String getId() {
            return partialOp.getId();
        }
    }

    static class PartialFilterOperationStarted<F> extends PartialFilterOperationRelated<F> implements Start {

        private final PhaseSelector phaseSelector;

        /** Not using original object because it is mutable and currently not cloneable. */
        private final String queryItemsSpecDump;

        PartialFilterOperationStarted(
                @NotNull EnforcerFilterOperation<?, F>.PartialOp partialOp,
                @NotNull PhaseSelector phaseSelector,
                @NotNull String queryItemsSpecDump) {
            super(partialOp, null, null);
            this.phaseSelector = phaseSelector;
            this.queryItemsSpecDump = queryItemsSpecDump;
        }

        @Override
        public @NotNull TraceRecord defaultTraceRecord() {
            return TraceRecord.of(
                    "Starting partial filter determination (%s) for '%s', initial query items specification: %s".formatted(
                            operation.getDesc(),
                            phaseSelector,
                            queryItemsSpecDump));
        }
    }

    static class PartialFilterOperationNote<F> extends PartialFilterOperationRelated<F> {

        PartialFilterOperationNote(
                @NotNull EnforcerFilterOperation<?, F>.PartialOp partialOp,
                @Nullable String message,
                @Nullable Object[] arguments) {
            super(partialOp, message, arguments);
        }

        @Override
        public @NotNull TraceRecord defaultTraceRecord() {
            return TraceRecord.of(getFormattedMessage("", ""));
        }
    }

    static class PartialFilterOperationFinished<F> extends PartialFilterOperationRelated<F> implements End {

        private final EnforcerFilterOperation<?, F>.PartialOp partialOp;
        private final PhaseSelector phaseSelector;
        private final F filter;

        PartialFilterOperationFinished(
                @NotNull EnforcerFilterOperation<?, F>.PartialOp partialOp,
                @NotNull PhaseSelector phaseSelector,
                @NotNull F filter,
                @Nullable String message,
                Object... arguments) {
            super(partialOp, message, arguments);
            this.partialOp = partialOp;
            this.phaseSelector = phaseSelector;
            this.filter = filter;
        }

        @Override
        public @NotNull TraceRecord defaultTraceRecord() {
            String firstLine = "Finished partial filter determination (%s) for '%s'%s".formatted(
                    operation.getDesc(),
                    phaseSelector,
                    getFormattedMessage(": ", ""));
            List<String> nextLines = List.of(
                    "Resulting filter:\n" + operation.debugDumpFilter(filter, 1),
                    "Total 'allow' filter:\n" + operation.debugDumpFilter(partialOp.getSecurityFilterAllow(), 1),
                    "Total 'deny' filter:\n" + operation.debugDumpFilter(partialOp.getSecurityFilterDeny(), 1),
                    "Paths: " + partialOp.getQueryObjectsAutzCoverage().shortDump());
            return TraceRecord.of(
                    firstLine,
                    String.join("\n", nextLines));
        }
    }

    public static class PhasedDecisionOperationStarted extends OperationRelated<EnforcerDecisionOperation> implements Start {

        private final AuthorizationPhaseType phase;

        PhasedDecisionOperationStarted(
                @NotNull EnforcerDecisionOperation operation,
                @NotNull AuthorizationPhaseType phase) {
            super(operation, null, null);
            this.phase = phase;
        }

        @Override
        public @NotNull TraceRecord defaultTraceRecord() {
            return TraceRecord.of("START access decision for principal=%s, op=%s, phase=%s, %s".formatted(
                    operation.username,
                    prettyActionUrl(operation.operationUrl),
                    phase,
                    operation.params.shortDump()));
        }
    }

    public static class PhasedDecisionOperationFinished extends OperationRelated<EnforcerDecisionOperation> implements End {

        @NotNull private final AuthorizationPhaseType phase;
        @NotNull private final AccessDecision decision;

        PhasedDecisionOperationFinished(
                @NotNull EnforcerDecisionOperation operation,
                @NotNull AuthorizationPhaseType phase,
                @NotNull AccessDecision decision) {
            super(operation, null, null);
            this.phase = phase;
            this.decision = decision;
        }

        @Override
        public @NotNull TraceRecord defaultTraceRecord() {
            return TraceRecord.of("END access decision for principal=%s, op=%s, phase=%s: %s".formatted(
                    operation.username,
                    prettyActionUrl(operation.operationUrl),
                    phase,
                    decision));
        }
    }

    public static class PhasedDecisionOperationNote extends OperationRelated<EnforcerDecisionOperation> {

        @NotNull private final AuthorizationPhaseType phase;

        PhasedDecisionOperationNote(
                @NotNull EnforcerDecisionOperation operation,
                @NotNull AuthorizationPhaseType phase,
                @Nullable String message,
                @Nullable Object... arguments) {
            super(operation, message, arguments);
            this.phase = phase;
        }

        @Override
        public @NotNull TraceRecord defaultTraceRecord() {
            return TraceRecord.of(getFormattedMessage("", ""));
        }

        public @NotNull AuthorizationPhaseType getPhase() {
            return phase;
        }
    }

    static class CompileObjectSecurityConstraintsStarted extends OperationRelated<CompileConstraintsOperation<?>> implements Start {

        private final PrismObject<?> object;

        CompileObjectSecurityConstraintsStarted(
                @NotNull CompileConstraintsOperation<?> operation,
                @NotNull PrismObject<?> object) {
            super(operation, null, null);
            this.object = object;
        }

        @Override
        public @NotNull TraceRecord defaultTraceRecord() {
            return TraceRecord.of(
                    "Compiling security constraints for principal=%s, object=%s".formatted(operation.username, object));
        }
    }

    static class CompileObjectSecurityConstraintsFinished extends OperationRelated<CompileConstraintsOperation<?>> implements End {

        @NotNull private final PrismObject<?> object;
        @NotNull private final ObjectSecurityConstraintsImpl constraints;

        CompileObjectSecurityConstraintsFinished(
                @NotNull CompileConstraintsOperation<?> operation,
                @NotNull PrismObject<?> object,
                @NotNull ObjectSecurityConstraintsImpl constraints) {
            super(operation, null, null);
            this.object = object;
            this.constraints = constraints;
        }

        @Override
        public @NotNull TraceRecord defaultTraceRecord() {
            return TraceRecord.of(
                    "Compiled security constraints for principal=%s, object=%s".formatted(operation.username, object),
                    constraints.debugDump(1));
        }
    }

    static class CompileValueOperationConstraintsStarted extends OperationRelated<CompileConstraintsOperation<?>> implements Start {

        private final PrismObjectValue<?> object;
        private final @NotNull String[] actionUrls;

        CompileValueOperationConstraintsStarted(
                @NotNull CompileConstraintsOperation<?> operation,
                @NotNull PrismObjectValue<?> object,
                @NotNull String[] actionUrls) {
            super(operation, null, null);
            this.object = object;
            this.actionUrls = actionUrls;
        }

        @Override
        public @NotNull TraceRecord defaultTraceRecord() {
            return TraceRecord.of(
                    "Compiling security constraints for principal=%s, value=%s, actionUrls=%s".formatted(
                            operation.username,
                            object,
                            prettyActionUrl(actionUrls)));
        }
    }

    static class CompileValueOperationConstraintsFinished extends OperationRelated<CompileConstraintsOperation<?>> implements End {

        @NotNull private final PrismObjectValue<?> object;
        @NotNull private final UpdatablePrismEntityOpConstraints.ForValueContent constraints;

        CompileValueOperationConstraintsFinished(
                @NotNull CompileConstraintsOperation<?> operation,
                @NotNull PrismObjectValue<?> object,
                @NotNull UpdatablePrismEntityOpConstraints.ForValueContent constraints) {
            super(operation, null, null);
            this.object = object;
            this.constraints = constraints;
        }

        @Override
        public @NotNull TraceRecord defaultTraceRecord() {
            return TraceRecord.of(
                    "Compiled security constraints for principal=%s, value=%s".formatted(
                            operation.username,
                            object),
                    constraints.debugDump(1));
        }
    }

    static abstract class AuthorizationRelated extends SecurityTraceEvent {

        @NotNull final AuthorizationEvaluation authorizationEvaluation;

        AuthorizationRelated(
                @NotNull AuthorizationEvaluation authorizationEvaluation,
                @Nullable String message,
                @Nullable Object[] arguments) {
            super(message, arguments);
            this.authorizationEvaluation = authorizationEvaluation;
        }

        public @NotNull String getId() {
            return authorizationEvaluation.getId();
        }
    }

    static class AuthorizationProcessingStarted extends AuthorizationRelated implements Start {

        AuthorizationProcessingStarted(@NotNull AuthorizationEvaluation authorizationEvaluation) {
            super(authorizationEvaluation, null, null);
        }

        @Override
        public @NotNull TraceRecord defaultTraceRecord() {
            return TraceRecord.of("Evaluating " + authorizationEvaluation.getDesc());
        }
    }

    static class AuthorizationProcessingFinished extends AuthorizationRelated implements End {

        AuthorizationProcessingFinished(
                @NotNull AuthorizationEvaluation authorizationEvaluation,
                @Nullable String message,
                @Nullable Object... arguments) {
            super(authorizationEvaluation, message, arguments);
        }

        @Override
        public @NotNull TraceRecord defaultTraceRecord() {
            return TraceRecord.of("End of evaluation of %s%s".formatted(
                    authorizationEvaluation.getDesc(),
                    getFormattedMessage(": ", "")));
        }
    }

    static class AuthorizationFilterProcessingFinished extends AuthorizationRelated implements End {

        AuthorizationFilterProcessingFinished(
                @NotNull AuthorizationFilterEvaluation<?> authorizationEvaluation,
                @Nullable String message,
                @Nullable Object... arguments) {
            super(authorizationEvaluation, message, arguments);
        }

        @Override
        public @NotNull TraceRecord defaultTraceRecord() {
            var filterEval = ((AuthorizationFilterEvaluation<?>) authorizationEvaluation);
            return TraceRecord.of("Finished deriving filter from %s (applicable: %s)%s:".formatted(
                            authorizationEvaluation.getDesc(),
                            filterEval.isApplicable(),
                            getFormattedMessage(" [", "]")),
                    DebugUtil.debugDump(filterEval.getAutzFilter(), 1));
        }
    }

    /** Just an intermediate event (noting something). */
    static class AuthorizationProcessingEvent extends AuthorizationRelated {

        AuthorizationProcessingEvent(
                @NotNull AuthorizationEvaluation authorizationEvaluation,
                @Nullable String message,
                @Nullable Object... arguments) {
            super(authorizationEvaluation, message, arguments);
        }

        @Override
        public @NotNull TraceRecord defaultTraceRecord() {
            return TraceRecord.of(getFormattedMessage("", ""));
        }
    }
}
