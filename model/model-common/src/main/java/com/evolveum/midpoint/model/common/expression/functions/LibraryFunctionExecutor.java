/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.expression.functions;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.evolveum.midpoint.model.common.expression.functions.FunctionLibraryManager.FunctionInLibrary;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.task.api.Task;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.common.ModelCommonBeans;
import com.evolveum.midpoint.model.common.expression.evaluator.FunctionExpressionEvaluator;
import com.evolveum.midpoint.model.common.expression.script.ScriptExpressionEvaluationContext;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.repo.common.expression.Expression;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.config.FunctionConfigItem;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Executes a function from a function library.
 *
 * Serves as an interface between scripts (e.g., in Groovy) and a {@link FunctionLibrary}.
 *
 * The processing is somewhat similar to the one in {@link FunctionExpressionEvaluator}.
 */
public class LibraryFunctionExecutor {

    private static final Trace LOGGER = TraceManager.getTrace(LibraryFunctionExecutor.class);

    private final ExpressionFactory expressionFactory;
    private final FunctionLibrary library;
    private final FunctionLibraryManager functionLibraryManager = ModelCommonBeans.get().functionLibraryManager;

    LibraryFunctionExecutor(FunctionLibrary library, ExpressionFactory expressionFactory) {
        this.library = library;
        this.expressionFactory = expressionFactory;
    }

    /**
     * This method is invoked by the scripts. It is supposed to be only public method exposed
     * by this class.
     */
    public <V extends PrismValue, D extends ItemDefinition<?>> Object execute(
            @NotNull String functionName, @Nullable Map<String, Object> rawParams)
            throws ExpressionEvaluationException {

        Validate.notNull(functionName, "Function name must be specified");

        OperationResult result = ScriptExpressionEvaluationContext.getOperationResultRequired();

        Map<String, Object> params = Objects.requireNonNullElseGet(rawParams, Map::of);
        Set<String> paramNames = params.keySet();

        try {
            FunctionConfigItem function =
                    library.findFunction(functionName, paramNames, "custom function evaluation");

            var callerProfile = ScriptExpressionEvaluationContext.getThreadLocalRequired().getExpressionProfile();
            functionLibraryManager.checkCallAllowed(
                    new FunctionInLibrary(function, library),
                    callerProfile);

            LOGGER.trace("function to execute {}", function);

            var task = ScriptExpressionEvaluationContext.getTaskRequired();
            D outputDefinition = ExpressionEvaluationUtil.prepareFunctionOutputDefinition(function);

            ExpressionProfile functionExpressionProfile =
                    functionLibraryManager.determineFunctionExpressionProfile(library, result);

            Expression<V, D> expression =
                    functionLibraryManager.createFunctionExpression(
                            function, outputDefinition, functionExpressionProfile, task, result);

            ExpressionEvaluationContext functionEvaluationContext =
                    createFunctionEvaluationContext(function, functionExpressionProfile, params, task);

            PrismValueDeltaSetTriple<V> outputTriple = expression.evaluate(functionEvaluationContext, result);
            LOGGER.trace("Result of the expression evaluation: {}", outputTriple);

            return ExpressionEvaluationUtil.getSingleRealValue(
                    outputTriple, outputDefinition, functionEvaluationContext.getContextDescription());

        } catch (SchemaException | ExpressionEvaluationException | ObjectNotFoundException | CommunicationException |
                ConfigurationException | SecurityViolationException e) {
            throw new ExpressionEvaluationException(e.getMessage(), e);
        }
    }

    private @NotNull ExpressionEvaluationContext createFunctionEvaluationContext(
            FunctionConfigItem function, ExpressionProfile functionExpressionProfile, Map<String, Object> params, Task task)
            throws SchemaException, ConfigurationException {
        VariablesMap variables = new VariablesMap();
        if (MapUtils.isNotEmpty(params)) {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                String argName = entry.getKey();
                Object argValue = entry.getValue();
                variables.put(argName, ExpressionEvaluationUtil.convertInput(argName, argValue, function));
            }
        }

        ExpressionEvaluationContext context =
                new ExpressionEvaluationContext(
                        null,
                        variables,
                        "custom function execute",
                        task);
        context.setExpressionProfile(functionExpressionProfile);
        context.setExpressionFactory(expressionFactory);
        return context;
    }
}
