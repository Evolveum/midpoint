/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.impl;

import static com.evolveum.midpoint.util.MiscUtil.argCheck;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.selector.eval.OwnerResolver;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.enforcer.api.CompileConstraintsOptions;
import com.evolveum.midpoint.security.enforcer.api.ObjectSecurityConstraints;
import com.evolveum.midpoint.security.enforcer.api.PrismEntityOpConstraints;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.security.enforcer.impl.SecurityTraceEvent.CompileObjectSecurityConstraintsFinished;
import com.evolveum.midpoint.security.enforcer.impl.SecurityTraceEvent.CompileObjectSecurityConstraintsStarted;
import com.evolveum.midpoint.security.enforcer.impl.SecurityTraceEvent.CompileValueOperationConstraintsFinished;
import com.evolveum.midpoint.security.enforcer.impl.SecurityTraceEvent.CompileValueOperationConstraintsStarted;
import com.evolveum.midpoint.security.enforcer.impl.prism.SinglePhasePrismEntityOpConstraintsImpl;
import com.evolveum.midpoint.security.enforcer.impl.prism.TwoPhasesPrismEntityOpConstraintsImpl;
import com.evolveum.midpoint.security.enforcer.impl.prism.UpdatablePrismEntityOpConstraints;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/** Covers {@link SecurityEnforcer} operations dealing with compiling constraints for given operation or operations. */
class CompileConstraintsOperation<O extends ObjectType> extends EnforcerOperation {

    @NotNull private final CompileConstraintsOptions options;

    CompileConstraintsOperation(
            @Nullable MidPointPrincipal principal,
            @Nullable OwnerResolver ownerResolver,
            @NotNull SecurityEnforcer.Options enforcerOptions,
            @NotNull Beans beans,
            @NotNull CompileConstraintsOptions compileConstraintsOptions,
            @NotNull Task task) {
        super(principal, ownerResolver, enforcerOptions, beans, task);
        this.options = compileConstraintsOptions;
    }

    @NotNull ObjectSecurityConstraints compileSecurityConstraints(PrismObject<O> object, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException{
        argCheck(object != null, "Cannot compile security constraints of null object");
        traceCompileObjectSecurityConstraintStarted(object);
        var objectSecurityConstraints = new ObjectSecurityConstraintsImpl();
        int i = 0;
        for (Authorization autz : getAuthorizations()) {
            var evaluation = new AuthorizationEvaluation(i++, autz, this, result);
            evaluation.traceStart();
            if (evaluation.isApplicableToObject(object)) {
                objectSecurityConstraints.applyAuthorization(autz);
                evaluation.traceEndApplied();
            } else {
                evaluation.traceEndNotApplicable();
            }
        }
        traceCompileObjectSecurityConstraintsFinished(object, objectSecurityConstraints);
        return objectSecurityConstraints;
    }

    @NotNull PrismEntityOpConstraints.ForValueContent compileValueOperationConstraints(
            @NotNull PrismObjectValue<?> value,
            @Nullable AuthorizationPhaseType phase,
            @NotNull String[] actionUrls,
            @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        traceCompileValueOperationConstraintsStarted(value, actionUrls);
        var constraints =
                phase != null ?
                        new SinglePhasePrismEntityOpConstraintsImpl.ForValueContent(phase)
                        : new TwoPhasesPrismEntityOpConstraintsImpl.ForValueContent();
        int i = 0;
        for (Authorization autz : getAuthorizations()) {
            var evaluation = new AuthorizationEvaluation(i++, autz, this, result);
            evaluation.traceStart();
            if (evaluation.isApplicableToActions(actionUrls)) {
                constraints.applyAuthorization(value, evaluation);
                evaluation.traceEndApplied();
            } else {
                evaluation.traceEndNotApplicable();
            }
        }
        traceCompileValueOperationConstraintsFinished(value, constraints);
        return constraints;
    }

    public @NotNull CompileConstraintsOptions getOptions() {
        return options;
    }

    private void traceCompileObjectSecurityConstraintStarted(@NotNull PrismObject<O> object) {
        if (tracer.isEnabled()) {
            tracer.trace(
                    new CompileObjectSecurityConstraintsStarted(this, object));
        }
    }

    private void traceCompileObjectSecurityConstraintsFinished(
            @NotNull PrismObject<O> object, @NotNull ObjectSecurityConstraintsImpl constraints) {
        if (tracer.isEnabled()) {
            tracer.trace(
                    new CompileObjectSecurityConstraintsFinished(this, object, constraints));
        }
    }

    private void traceCompileValueOperationConstraintsStarted(@NotNull PrismObjectValue<?> value, @NotNull String[] actionUrls) {
        if (tracer.isEnabled()) {
            tracer.trace(
                    new CompileValueOperationConstraintsStarted(this, value, actionUrls));
        }
    }

    private void traceCompileValueOperationConstraintsFinished(
            @NotNull PrismObjectValue<?> value, UpdatablePrismEntityOpConstraints.ForValueContent constraints) {
        if (tracer.isEnabled()) {
            tracer.trace(
                    new CompileValueOperationConstraintsFinished(this, value, constraints));
        }
    }
}
