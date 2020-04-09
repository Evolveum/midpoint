/*
 * Copyright (c) 2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.util;

import javax.annotation.PostConstruct;

import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartitionDefinitionType;

/**
 * This task handler does nothing. Used in the tests.
 *
 * @author Radovan Semancik
 *
 */
@Component
public class MockTaskHandler implements TaskHandler {

    public static final String HANDLER_URI = SchemaConstants.NS_MODEL + "/mock/handler-3";

    @Autowired private TaskManager taskManager;

    private static final Trace LOGGER = TraceManager.getTrace(MockTaskHandler.class);

    @PostConstruct
    private void initialize() {
        taskManager.registerHandler(HANDLER_URI, this);
    }

    @Override
    public TaskRunResult run(RunningTask task, TaskPartitionDefinitionType partition) {
        LOGGER.trace("MockTaskHandler.run starting");

        OperationResult opResult = new OperationResult(OperationConstants.RECONCILIATION);
        opResult.setStatus(OperationResultStatus.IN_PROGRESS);
        TaskRunResult runResult = new TaskRunResult();
        runResult.setOperationResult(opResult);

        opResult.recordSuccess();
        runResult.setRunResultStatus(TaskRunResultStatus.FINISHED);
        runResult.setProgress(1L);

        LOGGER.trace("MockTaskHandler.run ending");

        return runResult;
    }

    @Override
    public Long heartbeat(Task task) {
        return null;
    }

    @Override
    public void refreshStatus(Task task) {
        // Do nothing. Everything is fresh already.
    }

    @Override
    public String getCategoryName(Task task) {
        return TaskCategory.SYSTEM;
    }

    @Override
    public String getArchetypeOid() {
        return SystemObjectsType.ARCHETYPE_SYSTEM_TASK.value();
    }
}
