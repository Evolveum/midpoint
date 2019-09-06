/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.engine.actions;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.engine.EngineInvocationContext;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;

import javax.xml.datatype.XMLGregorianCalendar;

/**
 *
 */
public class CloseCaseAction extends InternalAction {

	private static final Trace LOGGER = TraceManager.getTrace(CloseCaseAction.class);
	private final String outcome;

	CloseCaseAction(EngineInvocationContext ctx, String outcome) {
		super(ctx);
		this.outcome = outcome;
	}

	@Override
	public Action execute(OperationResult result) {
		traceEnter(LOGGER);

		CaseType currentCase = ctx.getCurrentCase();

		XMLGregorianCalendar now = engine.clock.currentTimeXMLGregorianCalendar();
		for (CaseWorkItemType wi : currentCase.getWorkItem()) {
			if (wi.getCloseTimestamp() == null) {
				wi.setCloseTimestamp(now);
			}
		}

		String state = currentCase.getState();
		if (state == null || SchemaConstants.CASE_STATE_CREATED.equals(state) || SchemaConstants.CASE_STATE_OPEN.equals(state)) {
			currentCase.setOutcome(outcome);
			currentCase.setState(SchemaConstants.CASE_STATE_CLOSING);
			ctx.setWasClosed(true);
			// audit and notification is done after the onProcessEnd is finished
		} else {
			LOGGER.debug("Case {} was already closed; its state is {}", currentCase, state);
			result.recordWarning("Case was already closed");
		}

		traceExit(LOGGER, null);
		return null;
	}
}
