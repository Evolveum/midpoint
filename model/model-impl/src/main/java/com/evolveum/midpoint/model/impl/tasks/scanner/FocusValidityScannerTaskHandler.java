/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.tasks.scanner;

import javax.annotation.PostConstruct;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskLoggingOptionType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.model.impl.lens.Clockwork;
import com.evolveum.midpoint.model.impl.lens.ContextFactory;
import com.evolveum.midpoint.repo.common.task.TaskExecutionClass;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;

/**
 * Validity scanner task.
 *
 * @author Radovan Semancik
 */
@Component
@TaskExecutionClass(FocusValidityScannerTaskExecution.class)
// PartExecutionClass is not defined, because part executions are created in a custom way
public class FocusValidityScannerTaskHandler
        extends AbstractScannerTaskHandler<FocusValidityScannerTaskHandler, FocusValidityScannerTaskExecution> {

    private static final Trace LOGGER = TraceManager.getTrace(FocusValidityScannerTaskHandler.class);

    @Autowired protected ContextFactory contextFactory;
    @Autowired protected Clockwork clockwork;

    public FocusValidityScannerTaskHandler() {
        super(LOGGER, "Focus validity scan", OperationConstants.FOCUS_VALIDITY_SCAN);
        globalReportingOptions.setDefaultDetermineExpectedTotal(false); // To avoid problems like in MID-6934.
        globalReportingOptions.setDefaultBucketCompletionLogging(TaskLoggingOptionType.NONE); // To avoid log noise.
    }

    @PostConstruct
    private void initialize() {
        taskManager.registerHandler(ModelPublicConstants.FOCUS_VALIDITY_SCANNER_TASK_HANDLER_URI, this);
        taskManager.registerAdditionalHandlerUri(ModelPublicConstants.PARTITIONED_FOCUS_VALIDITY_SCANNER_TASK_HANDLER_URI, this);
        taskManager.registerAdditionalHandlerUri(ModelPublicConstants.PARTITIONED_FOCUS_VALIDITY_SCANNER_TASK_HANDLER_URI_1, this);
        taskManager.registerAdditionalHandlerUri(ModelPublicConstants.PARTITIONED_FOCUS_VALIDITY_SCANNER_TASK_HANDLER_URI_2, this);
        taskManager.registerDeprecatedHandlerUri(ModelPublicConstants.DEPRECATED_FOCUS_VALIDITY_SCANNER_TASK_HANDLER_URI, this);
    }

    @Override
    public String getArchetypeOid() {
        return SystemObjectsType.ARCHETYPE_SYSTEM_TASK.value();
    }

    @Override
    public String getDefaultChannel() {
        return null; // TODO decide
    }
}
