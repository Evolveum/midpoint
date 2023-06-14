/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.impl;

import static com.evolveum.midpoint.schema.GetOperationOptions.createAllowNotFoundCollection;
import static com.evolveum.midpoint.security.enforcer.impl.TracingUtil.*;
import static com.evolveum.midpoint.util.MiscUtil.getDiagInfo;

import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.selector.eval.*;
import com.evolveum.midpoint.schema.selector.spec.ValueSelector;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Treats given {@link ValueSelector}: evaluates its applicability or produces security filters based on it.
 *
 * Instantiated and used as part of an {@link EnforcerOperation}.
 */
class SelectorEvaluation implements SubjectedEvaluationContext, ObjectResolver {

    /** Using {@link SecurityEnforcerImpl} to ensure log compatibility. */
    private static final Trace LOGGER = TraceManager.getTrace(SecurityEnforcerImpl.class);

    @NotNull private final String id;
    @NotNull final ValueSelector selector;
    @Nullable private final PrismValue value;
    @NotNull final String desc;
    @NotNull final AuthorizationEvaluation authorizationEvaluation;
    @NotNull private final EnforcerOperation enforcerOp;
    @NotNull final Beans b;
    @NotNull private final OperationResult result;

    SelectorEvaluation(
            @NotNull String id,
            @NotNull ValueSelector selector,
            @Nullable PrismValue value,
            @NotNull String desc,
            @NotNull AuthorizationEvaluation authorizationEvaluation,
            @NotNull OperationResult result) {
        this.id = id;
        this.selector = selector;
        this.value = value;
        this.desc = desc;
        this.authorizationEvaluation = authorizationEvaluation;
        this.enforcerOp = authorizationEvaluation.op;
        this.b = enforcerOp.b;
        this.result = result;
    }

    boolean isSelectorApplicable()
            throws SchemaException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {

        var ctx = new MatchingContext(
                createFilterEvaluator(),
                new LoggingTracer(),
                b.repositoryService,
                this,
                enforcerOp.ownerResolver,
                this,
                ClauseProcessingContextDescription.defaultOne(id, desc),
                DelegatorSelection.NO_DELEGATOR);

        assert value != null;
        return selector.matches(value, ctx);
    }

    ObjectFilterExpressionEvaluator createFilterEvaluator() {
        return authorizationEvaluation.createFilterEvaluator(desc);
    }

    @Override
    public String getPrincipalOid() {
        return enforcerOp.getPrincipalOid();
    }

    @Override
    public FocusType getPrincipalFocus() {
        return enforcerOp.getPrincipalFocus();
    }

    public Object getDesc() {
        return desc;
    }

    @Override
    public @NotNull Set<String> getSelfOids(@NotNull DelegatorSelection delegatorSelection) {
        return enforcerOp.getAllSelfOids(delegatorSelection);
    }

    @Override
    public @NotNull Set<String> getSelfPlusRolesOids(@NotNull DelegatorSelection delegatorSelection) {
        return enforcerOp.getAllSelfPlusRolesOids(delegatorSelection);
    }

    public @Nullable OwnerResolver getOwnerResolver() {
        return enforcerOp.ownerResolver;
    }

    public @NotNull RepositoryService getRepositoryService() {
        return b.repositoryService;
    }

    /** TODO */
    public PrismObject<? extends ObjectType> resolveReference(
            ObjectReferenceType ref, Object context, String referenceName) {
        if (ref != null && ref.getOid() != null) {
            Class<? extends ObjectType> type = ref.getType() != null ?
                    b.prismContext.getSchemaRegistry().getCompileTimeClass(ref.getType()) : UserType.class;
            try {
                return b.repositoryService.getObject(type, ref.getOid(), createAllowNotFoundCollection(), result);
            } catch (ObjectNotFoundException | SchemaException e) {
                LoggingUtils.logExceptionAsWarning(
                        LOGGER, "Couldn't resolve {} of {}", e, referenceName, getDiagInfo(context));
                return null;
            }
        } else {
            return null;
        }
    }

    public String getSelectorId() {
        return id;
    }

    class LoggingTracer implements SelectorProcessingTracer {

        @Override
        public boolean isEnabled() {
            return enforcerOp.traceEnabled;
        }

        @Override
        public void trace(@NotNull TraceEvent event) {

            String typeMark;
            if (event instanceof TraceEvent.SelectorProcessingStarted) {
                typeMark = START;
            } else if (event instanceof TraceEvent.SelectorProcessingFinished) {
                typeMark = END;
            } else {
                typeMark = CONT;
            }

            String prefix = SEL + event.getDescription().getId() + typeMark;

            var record = event.defaultTraceRecord();
            var nextLines = record.getNextLines();
            if (nextLines == null) {
                LOGGER.trace("{}{}", prefix, record.getFirstLine());
            } else {
                LOGGER.trace("{}{}\n{}", prefix, record.getFirstLine(), nextLines);
            }
        }
    }
}
