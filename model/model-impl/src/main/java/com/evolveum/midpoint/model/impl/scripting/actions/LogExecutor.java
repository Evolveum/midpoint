/*
 * Copyright (c) 2010-2014 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.model.impl.scripting.actions;

import com.evolveum.midpoint.model.impl.scripting.PipelineData;
import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.model.api.ScriptExecutionException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionExpressionType;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @author mederly
 */
@Component
public class LogExecutor extends BaseActionExecutor {

    private static final Trace LOGGER = TraceManager.getTrace(LogExecutor.class);

    public static final String NAME = "log";
    public static final String PARAM_LEVEL = "level";
    public static final String PARAM_MESSAGE = "message";
    public static final String LEVEL_INFO = "info";
    public static final String LEVEL_DEBUG = "debug";
    public static final String LEVEL_TRACE = "trace";

    @PostConstruct
    public void init() {
        scriptingExpressionEvaluator.registerActionExecutor(NAME, this);
    }

    @Override
    public PipelineData execute(ActionExpressionType expression, PipelineData input, ExecutionContext context, OperationResult parentResult) throws ScriptExecutionException {

        String levelAsString = expressionHelper.getArgumentAsString(expression.getParameter(), PARAM_LEVEL, input, context, LEVEL_INFO, NAME, parentResult);
        String message = expressionHelper.getArgumentAsString(expression.getParameter(), PARAM_MESSAGE, input, context, "Current data: ", NAME, parentResult);
        message += "{}";

        if (LEVEL_INFO.equals(levelAsString)) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(message, DebugUtil.debugDump(input));
            }
        } else if (LEVEL_DEBUG.equals(levelAsString)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(message, DebugUtil.debugDump(input));
            }
        } else if (LEVEL_TRACE.equals(levelAsString)) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(message, DebugUtil.debugDump(input));
            }
        } else {
            LOGGER.warn("Invalid logging level specified for 'log' scripting action: " + levelAsString);
        }
        return input;
    }
}
