/*
 * Copyright (c) 2010-2019 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.wf.impl.engine.actions;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.engine.EngineInvocationContext;
import com.evolveum.midpoint.wf.impl.engine.WorkflowEngine;
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
			currentCase.setCloseTimestamp(now);
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
