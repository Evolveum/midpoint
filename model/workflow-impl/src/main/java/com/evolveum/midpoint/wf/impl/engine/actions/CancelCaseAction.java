/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.engine.actions;

import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.enforcer.api.AuthorizationParameters;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.api.request.CancelCaseRequest;
import com.evolveum.midpoint.wf.impl.engine.EngineInvocationContext;

/**
 *
 */
public class CancelCaseAction extends RequestedAction<CancelCaseRequest> {

    private static final Trace LOGGER = TraceManager.getTrace(CancelCaseAction.class);

    private static final String OP_EXECUTE = CancelCaseAction.class.getName() + ".execute";

    public CancelCaseAction(EngineInvocationContext ctx, CancelCaseRequest request) {
        super(ctx, request);
    }

    @Override
    public Action execute(OperationResult parentResult)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        OperationResult result = parentResult.subresult(OP_EXECUTE)
                .setMinor()
                .build();
        try {
            traceEnter(LOGGER);

            engine.securityEnforcer.authorize(ModelAuthorizationAction.CANCEL_CASE.getUrl(), null,
                    AuthorizationParameters.Builder.buildObject(ctx.getCurrentCase().asPrismObject()), null, ctx.getTask(),
                    result);

            // TODO consider putting some events and notifications here

            CloseCaseAction next = new CloseCaseAction(ctx, null);
            traceExit(LOGGER, next);
            return next;
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }
}
