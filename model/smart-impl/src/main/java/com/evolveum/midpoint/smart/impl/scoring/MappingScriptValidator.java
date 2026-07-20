/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.smart.impl.scoring;

import java.util.Collection;

import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;

/**
 * Validates if suggested mapping scripts are runnable by executing them with test data.
 */
@Component
public class MappingScriptValidator {

    private static final Trace LOGGER = TraceManager.getTrace(MappingScriptValidator.class);

    private static final String ID_TEST_MAPPING_SCRIPT = "testMappingScript";

    private final ExpressionFactory expressionFactory;

    public MappingScriptValidator(ExpressionFactory expressionFactory) {
        this.expressionFactory = expressionFactory;
    }

    /**
     * Tests whether the suggested mapping script can be executed successfully.
     * Executes the script with a sample test value (preserving its declared type) and catches any exceptions.
     */
    public void testCategoricalMappingScript(
            ExpressionType expression,
            String variableName,
            @Nullable Object testValue,
            Class<?> testValueClass,
            Task task,
            OperationResult parentResult) throws ScriptValidationException {

        if (expression == null) {
            return;
        }

        var result = parentResult.subresult(ID_TEST_MAPPING_SCRIPT)
                .addParam("variableName", variableName)
                .build();

        try {
            evaluateExpression(expression, variableName, testValue, testValueClass, task, result);
            LOGGER.debug("Mapping script validation successful");
        } catch (Exception e) {
            LOGGER.debug("Mapping script validation failed: {}", e.getMessage());
            throw new ScriptValidationException("Script validation failed: " + e.getMessage());
        } finally {
            result.close();
        }
    }

    /**
     * Evaluates a mapping expression with the provided variable, value and its declared type.
     *
     * Preserving the original type of the variable is important when the expression invokes
     * methods specific to that type.
     */
    public Collection<String> evaluateExpression(
            ExpressionType expressionType,
            String variableName,
            @Nullable Object value,
            Class<?> valueClass,
            Task task,
            OperationResult parentResult)
            throws ExpressionEvaluationException, SecurityViolationException, SchemaException,
            CommunicationException, ConfigurationException, ObjectNotFoundException {

        final String description = "Mapping expression evaluation";
        final VariablesMap variables = new VariablesMap();
        variables.put(variableName, value, valueClass);
        // Provide default iteration variables for validation
        variables.put(ExpressionConstants.VAR_ITERATION, 0, Integer.class);
        variables.put(ExpressionConstants.VAR_ITERATION_TOKEN, "", String.class);
        final ExpressionProfile profile = ExpressionProfile.safeScriptingOnly();

        return ExpressionUtil.evaluateStringExpression(
                variables,
                expressionType,
                profile,
                this.expressionFactory,
                description,
                task,
                parentResult);
    }

}
