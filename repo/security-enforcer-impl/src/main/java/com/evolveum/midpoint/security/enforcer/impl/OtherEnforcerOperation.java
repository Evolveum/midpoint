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
import com.evolveum.midpoint.security.enforcer.api.ObjectOperationConstraints;
import com.evolveum.midpoint.security.enforcer.api.ObjectSecurityConstraints;
import com.evolveum.midpoint.security.enforcer.api.PrismEntityOpConstraints;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.security.enforcer.impl.prism.SinglePhasePrismEntityOpConstraintsImpl;
import com.evolveum.midpoint.security.enforcer.impl.prism.TwoPhasesPrismEntityOpConstraintsImpl;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/** Covers {@link SecurityEnforcer} operations other than access decision or security filter computation. */
class OtherEnforcerOperation<O extends ObjectType> extends EnforcerOperation {

    OtherEnforcerOperation(
            @Nullable MidPointPrincipal principal,
            @Nullable OwnerResolver ownerResolver,
            @NotNull Beans beans,
            @NotNull Task task) {
        super(principal, ownerResolver, beans, task);
    }

    ObjectSecurityConstraints compileSecurityConstraints(PrismObject<O> object, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException{
        argCheck(object != null, "Cannot compile security constraints of null object");
        if (traceEnabled) {
            LOGGER.trace("SEC: evaluating security constraints principal={}, object={}", username, object);
        }
        var objectSecurityConstraints = new ObjectSecurityConstraintsImpl();
        int i = 0;
        for (Authorization autz : getAuthorizations()) {
            var evaluation = new AuthorizationEvaluation(String.valueOf(i++), autz, this, result);
            evaluation.traceStart();
            if (evaluation.isApplicableToObject(object)) {
                objectSecurityConstraints.applyAuthorization(autz);
            } else {
                evaluation.traceEndNotApplicable();
            }
        }
        if (traceEnabled) {
            LOGGER.trace("SEC: evaluated security constraints principal={}, object={}:\n{}",
                    username, object, objectSecurityConstraints.debugDump(1));
        }
        return objectSecurityConstraints;
    }

    ObjectOperationConstraints compileOperationConstraints(
            @NotNull PrismObject<O> object, String[] actionUrls, @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        argCheck(object != null, "Cannot compile security constraints of null object");
        if (traceEnabled) {
            LOGGER.trace("SEC: evaluating operation security constraints principal={}, object={}", username, object);
        }
        var constraints = new ObjectOperationConstraintsImpl();
        int i = 0;
        for (Authorization autz : getAuthorizations()) {
            var evaluation = new AuthorizationEvaluation(String.valueOf(i++), autz, this, result);
            evaluation.traceStart();
            if (evaluation.isApplicableToActions(actionUrls)
                    && evaluation.isApplicableToObject(object)) {
                constraints.applyAuthorization(autz);
            } else {
                evaluation.traceEndNotApplicable();
            }
        }
        if (traceEnabled) {
            LOGGER.trace("SEC: evaluated security constraints principal={}, object={}:\n{}",
                    username, object, constraints.debugDump(1));
        }
        return constraints;
    }

    @NotNull PrismEntityOpConstraints.ForValueContent compileValueOperationConstraints(
            @NotNull PrismObjectValue<?> value,
            @Nullable AuthorizationPhaseType phase,
            @NotNull String[] actionUrls,
            @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        if (traceEnabled) {
            LOGGER.trace("SEC> evaluating value operation security constraints principal={}, value={}, actionUrls={}",
                    username, value, actionUrls);
        }
        var constraints =
                phase != null ?
                        new SinglePhasePrismEntityOpConstraintsImpl.ForValueContent(phase)
                        : new TwoPhasesPrismEntityOpConstraintsImpl.ForValueContent();
        int i = 0;
        for (Authorization autz : getAuthorizations()) {
            var evaluation = new AuthorizationEvaluation(String.valueOf(i++), autz, this, result);
            evaluation.traceStart();
            if (evaluation.isApplicableToActions(actionUrls)) {
                constraints.applyAuthorization(value, evaluation);
            } else {
                evaluation.traceEndNotApplicable();
            }
        }
        if (traceEnabled) {
            LOGGER.trace("SEC= evaluated value operation constraints principal={}, object={}:\n{}",
                    username, value, constraints.debugDump(1));
        }
        return constraints;
    }
}
