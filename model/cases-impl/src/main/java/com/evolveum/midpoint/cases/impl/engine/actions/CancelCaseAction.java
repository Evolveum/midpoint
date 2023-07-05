/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.cases.impl.engine.actions;

import com.evolveum.midpoint.cases.impl.engine.CaseEngineOperationImpl;
import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.enforcer.api.AuthorizationParameters;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.cases.api.request.CancelCaseRequest;

import org.jetbrains.annotations.Nullable;

/**
 * Cancels given case.
 */
class CancelCaseAction extends RequestedAction<CancelCaseRequest> {

    private static final Trace LOGGER = TraceManager.getTrace(CancelCaseAction.class);

    public CancelCaseAction(CaseEngineOperationImpl operation, CancelCaseRequest request) {
        super(operation, request, LOGGER);
    }

    @Override
    public @Nullable Action executeInternal(OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {

        beans.securityEnforcer.authorize(
                ModelAuthorizationAction.CANCEL_CASE.getUrl(),
                null,
                AuthorizationParameters.Builder.buildObject(operation.getCurrentCase().asPrismObject()),
                operation.getTask(),
                result);

        // TODO consider putting some events and notifications here

        return new CloseCaseAction(operation, null);
    }
}
