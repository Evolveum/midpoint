/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.cases.impl.engine.actions;

import com.evolveum.midpoint.cases.impl.engine.CaseEngineOperationImpl;
import com.evolveum.midpoint.schema.util.cases.CaseState;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;

import org.jetbrains.annotations.Nullable;

import javax.xml.datatype.XMLGregorianCalendar;

/**
 * Closes a case. This is an internal action, so e.g. no authorizations are checked here.
 */
public class CloseCaseAction extends InternalAction {

    private static final Trace LOGGER = TraceManager.getTrace(CloseCaseAction.class);

    private final String outcomeUri;

    CloseCaseAction(CaseEngineOperationImpl ctx, String outcomeUri) {
        super(ctx, LOGGER);
        this.outcomeUri = outcomeUri;
    }

    @Override
    public @Nullable Action executeInternal(OperationResult result) {
        CaseType currentCase = operation.getCurrentCase();

        XMLGregorianCalendar now = beans.clock.currentTimeXMLGregorianCalendar();
        for (CaseWorkItemType wi : currentCase.getWorkItem()) {
            if (wi.getCloseTimestamp() == null) {
                wi.setCloseTimestamp(now);
            }
        }

        CaseState state = CaseState.of(currentCase);
        if (state.isCreated() || state.isOpen()) {
            currentCase.setOutcome(outcomeUri);
            currentCase.setState(SchemaConstants.CASE_STATE_CLOSING);
            // Auditing and notifications are emitted when the "closing action" in extension is done.
        } else {
            LOGGER.debug("Case {} was already closed; its state is {}", currentCase, state);
            result.recordWarning("Case was already closed");
        }
        return null;
    }
}
