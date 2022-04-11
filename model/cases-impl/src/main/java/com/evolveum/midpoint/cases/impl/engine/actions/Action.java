/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.cases.impl.engine.actions;

import com.evolveum.midpoint.cases.api.extensions.EngineExtension;
import com.evolveum.midpoint.cases.api.request.Request;
import com.evolveum.midpoint.cases.impl.engine.CaseBeans;
import com.evolveum.midpoint.cases.impl.engine.CaseEngineOperationImpl;
import com.evolveum.midpoint.cases.impl.engine.events.PendingAuditRecords;
import com.evolveum.midpoint.cases.impl.engine.events.PendingNotificationEvents;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;

import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Abstract action that has to be executed by the case engine.
 *
 * It can be either a {@link RequestedAction} (created from a {@link Request}),
 * or an {@link InternalAction} (created to fulfill internal demands).
 */
public abstract class Action {

    /** The operation this action is a part of. */
    @NotNull public final CaseEngineOperationImpl operation;

    /** Collection of pending audit records for the operation. */
    @NotNull protected final PendingAuditRecords auditRecords;

    /** Collection of pending notification events for the operation. */
    @NotNull final PendingNotificationEvents notificationEvents;

    /** Useful beans. */
    @NotNull public final CaseBeans beans;

    /** Specific logger for this action. */
    @NotNull private final Trace logger;

    Action(@NotNull CaseEngineOperationImpl operation, @NotNull Trace logger) {
        this.operation = operation;
        this.auditRecords = operation.getAuditRecords();
        this.notificationEvents = operation.getNotificationEvents();
        this.beans = operation.getBeans();
        this.logger = logger;
    }

    /**
     * Executes this action. This method is called only once during lifetime of this object!
     *
     * @return The next (follow-up) action that should be executed - or null of none.
     */
    public @Nullable Action execute(OperationResult parentResult)
            throws SchemaException, SecurityViolationException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException {
        OperationResult result = parentResult.subresult(getClass().getName() + ".execute")
                .setMinor()
                .build();
        try {
            traceEnter();
            Action nextAction = executeInternal(result);
            traceExit(nextAction);
            return nextAction;
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.close();
        }
    }

    /**
     * The core of the execution.
     *
     * @return The next action.
     */
    abstract @Nullable Action executeInternal(OperationResult result)
            throws SchemaException, SecurityViolationException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException;

    private void traceEnter() {
        logger.trace("+++ ENTER: ctx={}", operation);
    }

    private void traceExit(Action nextAction) {
        logger.trace("+++ EXIT: next={}, operation={}", nextAction, operation);
    }

    @NotNull CaseType getCurrentCase() {
        return operation.getCurrentCase();
    }

    @NotNull Task getTask() {
        return operation.getTask();
    }

    @NotNull EngineExtension getEngineExtension() {
        return operation.getEngineExtension();
    }
}
