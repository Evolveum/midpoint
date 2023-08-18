/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.scripting.actions;

import com.evolveum.midpoint.model.impl.scripting.PipelineData;
import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.util.exception.ScriptExecutionException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionExpressionType;

import com.evolveum.midpoint.xml.ns._public.model.scripting_3.LogActionExpressionType;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Executes "log" scripting action.
 */
@Component
public class LogExecutor extends BaseActionExecutor {

    private static final Trace LOGGER = TraceManager.getTrace(LogExecutor.class);

    public static final String NAME = "log";
    private static final String PARAM_LEVEL = "level";
    private static final String PARAM_MESSAGE = "message";
    private static final String LEVEL_INFO = "info";
    private static final String LEVEL_DEBUG = "debug";
    private static final String LEVEL_TRACE = "trace";

    private static final String DEFAULT_MESSAGE = "Current data: ";
    private static final String DEFAULT_LEVEL = LEVEL_INFO;

    @PostConstruct
    public void init() {
        actionExecutorRegistry.register(NAME, LogActionExpressionType.class,this);
    }

    @Override
    public PipelineData execute(ActionExpressionType expression, PipelineData input, ExecutionContext context,
            OperationResult globalResult) throws ScriptExecutionException, SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {

        String message = expressionHelper.getActionArgument(String.class, expression,
                LogActionExpressionType.F_MESSAGE, PARAM_MESSAGE, input, context, DEFAULT_MESSAGE, NAME, globalResult) + "{}";
        String level = expressionHelper.getActionArgument(String.class, expression,
                LogActionExpressionType.F_LEVEL, PARAM_LEVEL, input, context, DEFAULT_LEVEL, NAME, globalResult);

        if (LEVEL_INFO.equalsIgnoreCase(level)) {
            LOGGER.info(message, DebugUtil.debugDumpLazily(input));
        } else if (LEVEL_DEBUG.equalsIgnoreCase(level)) {
            LOGGER.debug(message, DebugUtil.debugDumpLazily(input));
        } else if (LEVEL_TRACE.equalsIgnoreCase(level)) {
            LOGGER.trace(message, DebugUtil.debugDumpLazily(input));
        } else {
            LOGGER.warn("Invalid logging level specified for 'log' scripting action: " + level);
        }
        return input;
    }

    @Override
    @NotNull String getLegacyActionName() {
        return NAME;
    }

    @Override
    @NotNull String getConfigurationElementName() {
        return SchemaConstantsGenerated.SC_LOG.getLocalPart();
    }
}
