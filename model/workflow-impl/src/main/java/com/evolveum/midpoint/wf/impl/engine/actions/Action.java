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

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.wf.impl.engine.EngineInvocationContext;
import com.evolveum.midpoint.wf.impl.engine.WorkflowEngine;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public abstract class Action {

	@NotNull public final WorkflowEngine engine;
	@NotNull public final EngineInvocationContext ctx;

	Action(@NotNull EngineInvocationContext ctx) {
		this.ctx = ctx;
		this.engine = ctx.getEngine();
	}

	public abstract Action execute(OperationResult result)
			throws SchemaException, SecurityViolationException, ObjectNotFoundException, CommunicationException,
			ConfigurationException, ExpressionEvaluationException;

	void traceEnter(Trace logger) {
		logger.trace("+++ ENTER: ctx={}", ctx);
	}

	void traceExit(Trace logger, Action next) {
		logger.trace("+++ EXIT: next={}, ctx={}", next, ctx);
	}
}
