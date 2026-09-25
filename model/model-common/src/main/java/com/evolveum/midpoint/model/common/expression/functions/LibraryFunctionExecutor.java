/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.expression.functions;

import java.util.*;

import com.evolveum.midpoint.model.common.expression.functions.FunctionLibraryManager.FunctionInLibrary;
import com.evolveum.midpoint.model.common.expression.script.ScriptExecutionContext;
import com.evolveum.midpoint.prism.Safe;
import com.evolveum.midpoint.task.api.Task;

import com.evolveum.midpoint.util.annotation.Experimental;

import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.common.ModelCommonBeans;
import com.evolveum.midpoint.model.common.expression.evaluator.FunctionExpressionEvaluator;
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
@Safe // this is a safe class, as it respects the expression profile
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
     * This method is invoked by the scripts. It is more general version that accepts parameters as a map.
     * The map keys are parameter names, the values are parameter values.
     *
     * The parameter names are used to disambiguate overloaded functions.
     *
     * This is the recommended way.
     */
    @Safe
    public Object execute(@NotNull String functionName, @Nullable Map<String, Object> rawParams)
            throws ExpressionEvaluationException, SecurityViolationException {
        return executeInternal(functionName, new Arguments.Named(rawParams));
    }

    /**
     * This method is invoked by the scripts. It is a convenience method that accepts argument values as a varargs array.
     *
     * Limitation: there can be only a single method with a given name, as we cannot disambiguate overloaded functions based on
     * parameter names. (We could do that based on number and types of values, but we're not there yet.)
     *
     * EXPERIMENTAL. Use with care. May be removed later.
     */
    @SuppressWarnings("unused") // used from scripts
    @Experimental
    @Safe
    public Object executeSimple(@NotNull String functionName, Object... rawParams)
            throws ExpressionEvaluationException, SecurityViolationException {
        return executeInternal(functionName, new Arguments.Unnamed(Arrays.asList(rawParams)));
    }

    /** Names + values, or simply values. */
    private interface Arguments {

        @Nullable Collection<String> getParameterNamesIfKnown();

        /**
         * Returns a map of parameter names to argument values. For cases when we have values only, the caller provides
         * expected parameter names. If we have names + values, we are NOT obliged to check that the provided names match
         * the expected names.
         */
        @NotNull Map<String, Object> getArgumentsMap(List<String> parameterNames) throws ExpressionEvaluationException;

        /** Caller provided names and values. */
        record Named(@NotNull Map<String, Object> rawParams) implements Arguments {
            public Named(@Nullable Map<String, Object> rawParams) {
                this.rawParams = new HashMap<>(Objects.requireNonNullElseGet(rawParams, Map::of));
            }

            @Override
            public @NotNull Collection<String> getParameterNamesIfKnown() {
                return rawParams.keySet();
            }

            @Override
            public @NotNull Map<String, Object> getArgumentsMap(List<String> ignored) {
                return rawParams;
            }
        }

        /** Caller provided values only. */
        record Unnamed(@NotNull List<Object> rawParams) implements Arguments {
            public Unnamed(@NotNull List<Object> rawParams) {
                this.rawParams = List.copyOf(rawParams);
            }

            @Override
            public @Nullable Collection<String> getParameterNamesIfKnown() {
                return null;
            }

            @Override
            public @NotNull Map<String, Object> getArgumentsMap(List<String> parameterNames)
                    throws ExpressionEvaluationException {
                if (rawParams.size() != parameterNames.size()) {
                    throw new ExpressionEvaluationException(
                            "Number of provided arguments (" + rawParams.size()
                                    + ") does not match the number of parameters (" + parameterNames.size() + ")");
                }
                Map<String, Object> argumentsMap = new HashMap<>();
                for (int i = 0; i < parameterNames.size(); i++) {
                    argumentsMap.put(parameterNames.get(i), rawParams.get(i));
                }
                return argumentsMap;
            }
        }
    }

    private <V extends PrismValue, D extends ItemDefinition<?>> Object executeInternal(
            @NotNull String functionName, @NotNull Arguments arguments)
            throws ExpressionEvaluationException, SecurityViolationException {

        Validate.notNull(functionName, "Function name must be specified");

        OperationResult result = ScriptExecutionContext.getOperationResultRequired();

        try {
            FunctionConfigItem function =
                    library.findFunction(
                            functionName, arguments.getParameterNamesIfKnown(), "custom function evaluation");

            var callerProfile = ScriptExecutionContext.getThreadLocalRequired().getExpressionProfile();
            functionLibraryManager.checkCallAllowed(
                    new FunctionInLibrary(function, library),
                    callerProfile);

            LOGGER.trace("function to execute {}", function);

            var task = ScriptExecutionContext.getTaskRequired();
            D outputDefinition = ExpressionEvaluationUtil.prepareFunctionOutputDefinition(function);

            Expression<V, D> expression =
                    functionLibraryManager.createFunctionExpression(function, outputDefinition, task, result);

            ExpressionEvaluationContext functionEvaluationContext = createFunctionEvaluationContext(function, arguments, task);

            PrismValueDeltaSetTriple<V> outputTriple = expression.evaluate(functionEvaluationContext, result);
            LOGGER.trace("Result of the expression evaluation: {}", outputTriple);

            return ExpressionEvaluationUtil.getSingleRealValue(
                    outputTriple, outputDefinition, functionEvaluationContext.getContextDescription());

        } catch (SchemaException | ExpressionEvaluationException | ObjectNotFoundException | CommunicationException |
                 ConfigurationException | SubscriptionComplianceException e) {
            throw new ExpressionEvaluationException(e.getMessage(), e);
        }
    }

    private @NotNull ExpressionEvaluationContext createFunctionEvaluationContext(
            FunctionConfigItem function, Arguments arguments, Task task)
            throws SchemaException, ConfigurationException, ExpressionEvaluationException {
        VariablesMap variables = new VariablesMap();
        var argumentsMap = arguments.getArgumentsMap(function.getParameterNames());
        for (Map.Entry<String, Object> entry : argumentsMap.entrySet()) {
            String argName = entry.getKey();
            Object argValue = entry.getValue();
            variables.put(argName, ExpressionEvaluationUtil.convertInput(argName, argValue, function));
        }

        ExpressionEvaluationContext context =
                new ExpressionEvaluationContext(
                        null,
                        variables,
                        "custom function execute",
                        task);
        context.setExpressionFactory(expressionFactory);
        return context;
    }
}
