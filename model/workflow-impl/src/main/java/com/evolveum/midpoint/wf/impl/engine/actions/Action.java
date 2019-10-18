/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
