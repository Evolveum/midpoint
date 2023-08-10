/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.expression.evaluator;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.common.ModelCommonBeans;
import com.evolveum.midpoint.model.common.expression.functions.FunctionLibraryManager;

import com.evolveum.midpoint.model.common.expression.functions.FunctionLibraryManager.FunctionInLibrary;
import com.evolveum.midpoint.model.common.expression.functions.LibraryFunctionExecutor;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationUtil;
import com.evolveum.midpoint.schema.config.*;

import com.evolveum.midpoint.schema.expression.ExpressionEvaluatorProfile;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.repo.common.expression.Expression;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.evaluator.AbstractExpressionEvaluator;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.Nullable;

/**
 * Calls specified library function expression. It is something like a macro: Arguments for the function call
 * (expression themselves) are evaluated into triples, which become additional sources for the function expression.
 * Then the function expression is evaluated and the output triple is returned as an output triple for the function
 * expression evaluation.
 *
 * FIXME there are not triples in the current implementation! Only plain values.
 *
 * Somewhat similar to (script-callable) {@link LibraryFunctionExecutor}.
 *
 * @author katkav
 * @author semancik
 */
public class FunctionExpressionEvaluator<V extends PrismValue, D extends ItemDefinition<?>>
        extends AbstractExpressionEvaluator<V, D, FunctionExpressionEvaluatorType> {

    protected static final String CLASS_DOT = FunctionExpressionEvaluator.class.getName() + ".";
    private static final String OP_EVALUATE = CLASS_DOT + "evaluate";
    private static final String OP_RESOLVE_PARAMETER_VALUE = CLASS_DOT + "resolveParameterValue";
    private static final String OP_EVALUATE_FUNCTION = CLASS_DOT + "evaluateFunction";

    @NotNull private final FunctionLibraryManager functionLibraryManager = ModelCommonBeans.get().functionLibraryManager;

    @NotNull private final FunctionExpressionEvaluatorConfigItem functionCallCI;

    FunctionExpressionEvaluator(
            @NotNull QName elementName,
            @NotNull FunctionExpressionEvaluatorType functionEvaluatorBean,
            @Nullable D outputDefinition,
            @NotNull Protector protector) {
        super(elementName, functionEvaluatorBean, outputDefinition, protector);
        this.functionCallCI = FunctionExpressionEvaluatorConfigItem.of(
                functionEvaluatorBean,
                ConfigurationItemOrigin.undetermined()); // TODO origin
    }

    @Override
    public PrismValueDeltaSetTriple<V> evaluate(ExpressionEvaluationContext context, OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        checkEvaluatorProfile(context);

        OperationResult result = parentResult.createMinorSubresult(OP_EVALUATE);
        try {
            FunctionInLibrary function =
                    functionLibraryManager.getFunction(functionCallCI, context.getContextDescription(), result);

            ExpressionProfile functionExpressionProfile =
                    functionLibraryManager.determineFunctionExpressionProfile(function.library(), result);

            Expression<V, D> functionExpression =
                    functionLibraryManager.createFunctionExpression(
                            function.function(), outputDefinition, functionExpressionProfile, context.getTask(), result);

            ExpressionEvaluationContext functionEvaluationContext =
                    createFunctionEvaluationContext(function, functionExpressionProfile, context, result);

            return evaluateFunctionExpression(functionExpression, functionEvaluationContext, result);
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    private @NotNull ExpressionEvaluationContext createFunctionEvaluationContext(
            FunctionInLibrary functionInLibrary, @NotNull ExpressionProfile functionExpressionProfile,
            ExpressionEvaluationContext context, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException {

        VariablesMap functionVariables = new VariablesMap();

        for (FunctionCallArgumentConfigItem argument : functionCallCI.getArguments()) {
            ExpressionConfigItem argumentExpressionCI = argument.getExpression();
            String argumentName = argument.getName();
            OperationResult argumentResult = parentResult.subresult(OP_RESOLVE_PARAMETER_VALUE)
                    .setMinor()
                    .addArbitraryObjectAsParam("argumentExpression", argumentExpressionCI)
                    .build();
            try {
                String shortDesc = "argument " + argumentName + " evaluation in " + context.getContextDescription();

                // TODO find better name + simplify
                D argumentValueDefinition = ExpressionEvaluationUtil.determineVariableOutputDefinition(
                        functionInLibrary.function(), argumentName);

                Expression<V, D> argumentExpression = context.getExpressionFactory()
                        .makeExpression(
                                argumentExpressionCI,
                                argumentValueDefinition,
                                context.getExpressionProfile(), // this is the caller's profile
                                shortDesc, context.getTask(), argumentResult);

                PrismValueDeltaSetTriple<V> argumentValueTriple = argumentExpression.evaluate(context, argumentResult);

                // TODO why simple value here, not a triple?
                V argumentValue = ExpressionUtil.getExpressionOutputValue(argumentValueTriple, shortDesc);

                functionVariables.addVariableDefinition(argumentName, argumentValue, argumentValueDefinition);

            } catch (Exception e) {
                argumentResult.recordException("Failed to resolve argument: " + argumentName + ". Reason: " + e.getMessage(), e);
                throw e;
            } finally {
                argumentResult.close();
            }
        }

        ExpressionEvaluationContext functionContext = context.shallowClone();
        functionContext.setVariables(functionVariables);
        functionContext.setExpressionProfile(functionExpressionProfile);
        // to be sure it gets initialized correctly
        functionContext.setExpressionEvaluatorProfile(ExpressionEvaluatorProfile.forbidden());
        return functionContext;
    }

    private PrismValueDeltaSetTriple<V> evaluateFunctionExpression(Expression<V, D> functionExpression,
            ExpressionEvaluationContext functionContext, OperationResult parentResult) throws SchemaException,
            ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException {
        OperationResult result = parentResult.createMinorSubresult(OP_EVALUATE_FUNCTION);
        try {
            return functionExpression.evaluate(functionContext, parentResult);
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    @Override
    public String shortDebugDump() {
        return "function: " + expressionEvaluatorBean.getName();
    }
}
