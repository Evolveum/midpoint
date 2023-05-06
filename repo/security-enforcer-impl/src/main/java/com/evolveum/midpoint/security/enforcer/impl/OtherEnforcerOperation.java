/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.impl;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.OwnerResolver;
import com.evolveum.midpoint.security.enforcer.api.ObjectOperationConstraints;
import com.evolveum.midpoint.security.enforcer.api.ObjectSecurityConstraints;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.Task;

import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

import static com.evolveum.midpoint.util.MiscUtil.argCheck;

/** Covers {@link SecurityEnforcer} operations other than access decision or security filter computation. */
class OtherEnforcerOperation<O extends ObjectType> extends EnforcerOperation<O> {

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
            LOGGER.trace("AUTZ: evaluating security constraints principal={}, object={}", username, object);
        }
        var objectSecurityConstraints = new ObjectSecurityConstraintsImpl();
        for (Authorization autz : getAuthorizations()) {
            var evaluation = new AuthorizationEvaluation(autz, this, result);
            if (evaluation.isApplicableToObject(object)) {
                objectSecurityConstraints.applyAuthorization(autz);
            }
        }
        if (traceEnabled) {
            LOGGER.trace("AUTZ: evaluated security constraints principal={}, object={}:\n{}",
                    username, object, objectSecurityConstraints.debugDump(1));
        }
        return objectSecurityConstraints;
    }

    ObjectOperationConstraints compileOperationConstraints(
            @NotNull PrismObject<O> object, Collection<String> actionUrls, @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        argCheck(object != null, "Cannot compile security constraints of null object");
        if (traceEnabled) {
            LOGGER.trace("AUTZ: evaluating operation security constraints principal={}, object={}", username, object);
        }
        var constraints = new ObjectOperationConstraintsImpl();
        for (Authorization autz : getAuthorizations()) {
            var evaluation = new AuthorizationEvaluation(autz, this, result);
            if (evaluation.isApplicableToActions(actionUrls)
                    && evaluation.isApplicableToObject(object)) {
                constraints.applyAuthorization(autz);
            }
        }
        if (traceEnabled) {
            LOGGER.trace("AUTZ: evaluated security constraints principal={}, object={}:\n{}",
                    username, object, constraints.debugDump(1));
        }
        return constraints;
    }
}
