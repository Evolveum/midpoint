/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.expression.evaluator;

import java.util.List;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.MutablePrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.repo.common.expression.Expression;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.evaluator.AbstractExpressionEvaluator;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Calls specified custom function expression. It is something like a macro: Arguments for the function call
 * (expression themselves) are evaluated into triples, which become additional sources for the function expression.
 * Then the function expression is evaluated and the output triple is returned as an output triple for the function
 * expression evaluation.
 *
 * @author katkav
 * @author semancik
 */
public class FunctionExpressionEvaluator<V extends PrismValue, D extends ItemDefinition<?>>
        extends AbstractExpressionEvaluator<V, D, FunctionExpressionEvaluatorType> {

    private static final String OP_EVALUATE = FunctionExpressionEvaluator.class.getSimpleName() + ".evaluate";
    private static final String OP_GET_FUNCTION_TO_EXECUTE = FunctionExpressionEvaluator.class.getSimpleName() + ".getFunctionToExecute";
    private static final String OP_MAKE_EXPRESSION = FunctionExpressionEvaluator.class.getSimpleName() + ".makeExpression";
    private static final String OP_RESOLVE_PARAMETER_VALUE = FunctionExpressionEvaluator.class.getSimpleName() + ".resolveParameterValue";
    private static final String OP_EVALUATE_FUNCTION = FunctionExpressionEvaluator.class.getSimpleName() + ".evaluateFunction";

    private final ObjectResolver objectResolver;

    FunctionExpressionEvaluator(
            QName elementName,
            FunctionExpressionEvaluatorType functionEvaluatorType,
            D outputDefinition,
            Protector protector,
            ObjectResolver objectResolver) {
        super(elementName, functionEvaluatorType, outputDefinition, protector);
        this.objectResolver = objectResolver;
    }

    @Override
    public PrismValueDeltaSetTriple<V> evaluate(ExpressionEvaluationContext context, OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
        checkEvaluatorProfile(context);

        ObjectReferenceType functionLibraryRef = expressionEvaluatorBean.getLibraryRef();
        if (functionLibraryRef == null) {
            throw new SchemaException("No functions library defined in " + context.getContextDescription());
        }

        OperationResult result = parentResult.createMinorSubresult(OP_EVALUATE);
        try {
            ExpressionType functionExpressionBean = getFunctionExpressionBean(functionLibraryRef, context, result);
            Expression<V, D> functionExpression = createFunctionExpression(functionExpressionBean, context, result);
            ExpressionEvaluationContext functionContext = createFunctionEvaluationContext(functionExpressionBean, context, result);

            return evaluateFunctionExpression(functionExpression, functionContext, result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private ExpressionType getFunctionExpressionBean(ObjectReferenceType functionLibraryRef, ExpressionEvaluationContext context,
            OperationResult parentResult) throws ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        Task task = context.getTask();
        OperationResult result = parentResult.createMinorSubresult(OP_GET_FUNCTION_TO_EXECUTE);
        try {
            FunctionLibraryType functionLibrary = objectResolver.resolve(functionLibraryRef, FunctionLibraryType.class,
                    null, "resolving value policy reference in generateExpressionEvaluator", task, result);

            List<ExpressionType> allFunctions = functionLibrary.getFunction();
            if (CollectionUtils.isEmpty(allFunctions)) {
                throw new ConfigurationException(
                        "No functions defined in referenced function library: " + functionLibrary + " used in " +
                                context.getContextDescription());
            }

            String functionName = expressionEvaluatorBean.getName();
            if (StringUtils.isEmpty(functionName)) {
                throw new ConfigurationException(
                        "Missing function name in " + shortDebugDump() + " in " + context.getContextDescription());
            }

            List<ExpressionType> functionsMatchingName = allFunctions.stream()
                    .filter(expression -> functionName.equals(expression.getName()))
                    .collect(Collectors.toList());
            if (functionsMatchingName.isEmpty()) {
                String allFunctionNames = allFunctions.stream()
                        .map(ExpressionType::getName)
                        .collect(Collectors.joining(", "));
                throw new ConfigurationException("No function with name " + functionName + " found in " + shortDebugDump()
                        + ". Function defined are: " + allFunctionNames + ". In " + context.getContextDescription());
            }

            return selectFromMatchingFunctions(functionsMatchingName, context);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @NotNull
    private ExpressionType selectFromMatchingFunctions(List<ExpressionType> functionsMatchingName,
            ExpressionEvaluationContext context) throws ConfigurationException {

        if (functionsMatchingName.size() == 1) {
            return functionsMatchingName.iterator().next();
        }

        List<ExpressionParameterType> functionParams = expressionEvaluatorBean.getParameter();

        for (ExpressionType filteredExpression : functionsMatchingName) {
            List<ExpressionParameterType> filteredExpressionParameters = filteredExpression.getParameter();
            if (functionParams.size() != filteredExpressionParameters.size()) {
                continue;
            }
            if (!compareParameters(functionParams, filteredExpressionParameters)) {
                continue;
            }
            return filteredExpression;
        }
        String functionName = expressionEvaluatorBean.getName();
        throw new ConfigurationException("No matching function with name " + functionName + " found in " + shortDebugDump()
                + ". " + functionsMatchingName.size() + " functions with this name found but none matches the parameters. In " +
                context.getContextDescription());
    }

    private boolean compareParameters(List<ExpressionParameterType> functionParams, List<ExpressionParameterType> filteredExpressionParameters) {
        for (ExpressionParameterType functionParam : functionParams) {
            boolean found = false;
            for (ExpressionParameterType filteredExpressionParam : filteredExpressionParameters) {
                if (filteredExpressionParam.getName().equals(functionParam.getName())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    private Expression<V, D> createFunctionExpression(ExpressionType functionToExecute, ExpressionEvaluationContext context,
            OperationResult parentResult) throws SecurityViolationException, SchemaException, ObjectNotFoundException {
        OperationResult result = parentResult.createMinorSubresult(OP_MAKE_EXPRESSION);
        try {
            // TODO: expression profile should be determined from the function library archetype
            ExpressionProfile expressionProfile = MiscSchemaUtil.getExpressionProfile();

            return context.getExpressionFactory()
                    .makeExpression(functionToExecute, outputDefinition, expressionProfile,
                            "function execution", context.getTask(), result);
        } catch (SchemaException | ObjectNotFoundException e) {
            result.recordFatalError("Cannot make expression for " + functionToExecute + ". Reason: " + e.getMessage(), e);
            throw e;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @NotNull
    private ExpressionEvaluationContext createFunctionEvaluationContext(ExpressionType functionExpressionBean,
            ExpressionEvaluationContext context, OperationResult parentResult) throws SchemaException, ObjectNotFoundException,
            SecurityViolationException, ExpressionEvaluationException, CommunicationException, ConfigurationException {
        ExpressionEvaluationContext functionContext = context.shallowClone();
        VariablesMap functionVariables = new VariablesMap();

        for (ExpressionParameterType param : expressionEvaluatorBean.getParameter()) {
            ExpressionType parameterExpressionBean = param.getExpression();
            OperationResult variableResult = parentResult.createMinorSubresult(OP_RESOLVE_PARAMETER_VALUE);
            Expression<V, D> parameterExpression = null;
            try {
                variableResult.addArbitraryObjectAsParam("parameterExpression", parameterExpressionBean);
                D variableOutputDefinition = determineVariableOutputDefinition(functionExpressionBean, param.getName());

                // TODO: expression profile should be determined somehow
                ExpressionProfile expressionProfile = MiscSchemaUtil.getExpressionProfile();

                parameterExpression = context.getExpressionFactory()
                        .makeExpression(parameterExpressionBean, variableOutputDefinition, expressionProfile,
                                "parameter " + param.getName() + " evaluation in " + context.getContextDescription(),
                                context.getTask(), variableResult);
                PrismValueDeltaSetTriple<V> evaluatedValue = parameterExpression.evaluate(context, parentResult);
                V value = ExpressionUtil.getExpressionOutputValue(evaluatedValue, "evaluated value for parameter " + param.getName());
                functionVariables.addVariableDefinition(param.getName(), value, variableOutputDefinition);
            } catch (SchemaException | ExpressionEvaluationException | ObjectNotFoundException | CommunicationException
                    | ConfigurationException | SecurityViolationException e) {
                variableResult.recordFatalError("Failed to resolve variable: " + parameterExpression + ". Reason: " + e.getMessage());
                throw e;
            } finally {
                variableResult.computeStatusIfUnknown();
            }
        }

        functionContext.setVariables(functionVariables);
        return functionContext;
    }

    private D determineVariableOutputDefinition(ExpressionType functionToExecute, String paramName) throws SchemaException {
        ExpressionParameterType functionParameter = functionToExecute.getParameter().stream()
                .filter(param -> param.getName().equals(paramName))
                .findFirst()
                .orElseThrow(() -> new SchemaException("Unknown parameter " + paramName + " for function: " + functionToExecute));

        QName returnType = functionParameter.getType();
        if (returnType == null) {
            throw new SchemaException("Cannot determine parameter output definition for " + functionParameter);
        }

        ItemDefinition<?> returnTypeDef = prismContext.getSchemaRegistry().findItemDefinitionByType(returnType);
        if (returnTypeDef != null) {
            //noinspection unchecked
            return (D) returnTypeDef;
        } else {
            MutablePrismPropertyDefinition<?> dynamicReturnTypeDef =
                    prismContext.definitionFactory().createPropertyDefinition(SchemaConstantsGenerated.C_VALUE, returnType);
            dynamicReturnTypeDef.setMaxOccurs(functionToExecute.getReturnMultiplicity() == ExpressionReturnMultiplicityType.SINGLE ? 1 : -1);
            //noinspection unchecked
            return (D) dynamicReturnTypeDef;
        }
    }

    private PrismValueDeltaSetTriple<V> evaluateFunctionExpression(Expression<V, D> functionExpression,
            ExpressionEvaluationContext functionContext, OperationResult parentResult) throws SchemaException,
            ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException {
        OperationResult result = parentResult.createMinorSubresult(OP_EVALUATE_FUNCTION);
        try {
            return functionExpression.evaluate(functionContext, parentResult);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    public String shortDebugDump() {
        return "function: " + expressionEvaluatorBean.getName();
    }
}
