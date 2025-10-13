/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.cases.impl.engine.actions;

import com.evolveum.midpoint.cases.api.events.FutureNotificationEvent.CaseOpening;
import com.evolveum.midpoint.cases.impl.engine.CaseEngineOperationImpl;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.cases.CaseState;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.cases.api.request.OpenCaseRequest;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;

import org.jetbrains.annotations.Nullable;

/**
 * Opens the case. In normal case, proceeds with opening the first (or the only) stage.
 */
public class OpenCaseAction extends RequestedAction<OpenCaseRequest> {

    private static final Trace LOGGER = TraceManager.getTrace(OpenCaseAction.class);

    public OpenCaseAction(CaseEngineOperationImpl ctx, OpenCaseRequest request) {
        super(ctx, request, LOGGER);
    }

    @Override
    public @Nullable Action executeInternal(OperationResult result) {
        CaseType currentCase = operation.getCurrentCase();
        CaseState currentState = CaseState.of(currentCase);

        if (!currentState.isCreated()) {
            LOGGER.debug("Case was already opened; its state is {}", currentState);
            return null;
        }

        auditRecords.addCaseOpening(result);
        notificationEvents.add(
                new CaseOpening(currentCase));

        currentCase.setState(SchemaConstants.CASE_STATE_OPEN);

        // If there are zero stages, the case will be immediately closed by "open stage" action.
        return new OpenStageAction(operation);
    }
}
