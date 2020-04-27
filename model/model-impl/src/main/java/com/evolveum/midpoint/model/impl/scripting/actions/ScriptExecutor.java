/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.scripting.actions;

import com.evolveum.midpoint.model.api.PipelineItem;
import com.evolveum.midpoint.model.api.ScriptExecutionException;
import com.evolveum.midpoint.model.common.expression.script.ScriptExpression;
import com.evolveum.midpoint.model.common.expression.script.ScriptExpressionFactory;
import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.model.impl.scripting.PipelineData;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionSyntaxException;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.expression.TypedValue;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionExpressionType;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.List;

import static com.evolveum.midpoint.model.impl.scripting.VariablesUtil.cloneIfNecessary;

/**
 * @author mederly
 */
@Component
public class ScriptExecutor extends BaseActionExecutor {

    //private static final Trace LOGGER = TraceManager.getTrace(ScriptExecutor.class);

    @Autowired private ScriptExpressionFactory scriptExpressionFactory;
    @Autowired private ExpressionFactory expressionFactory;

    private static final String NAME = "execute-script";
    private static final String PARAM_SCRIPT = "script";
    private static final String PARAM_QUIET = "quiet";                  // todo implement for other actions as well
    private static final String PARAM_OUTPUT_ITEM = "outputItem";        // item name or type (as URI!) -- EXPERIMENTAL
    private static final String PARAM_FOR_WHOLE_INPUT = "forWholeInput";

    @PostConstruct
    public void init() {
        scriptingExpressionEvaluator.registerActionExecutor(NAME, this);
    }

    @Override
    public PipelineData execute(ActionExpressionType expression, PipelineData input, ExecutionContext context, OperationResult globalResult) throws ScriptExecutionException {

        checkRootAuthorization(context, globalResult, NAME);

        ScriptExpressionEvaluatorType script = expressionHelper.getSingleArgumentValue(expression.getParameter(), PARAM_SCRIPT, true, true,
                NAME, input, context, ScriptExpressionEvaluatorType.class, globalResult);
        String outputItem = expressionHelper.getSingleArgumentValue(expression.getParameter(), PARAM_OUTPUT_ITEM, false, false,
                NAME, input, context, String.class, globalResult);
        boolean forWholeInput = expressionHelper.getArgumentAsBoolean(expression.getParameter(), PARAM_FOR_WHOLE_INPUT, input, context, false, PARAM_FOR_WHOLE_INPUT, globalResult);
        boolean quiet = expressionHelper.getArgumentAsBoolean(expression.getParameter(), PARAM_QUIET, input, context, false, PARAM_QUIET, globalResult);

        ItemDefinition<?> outputDefinition = getItemDefinition(outputItem);
        ExpressionProfile expressionProfile = null; // TODO

        ScriptExpression scriptExpression;
        try {
            scriptExpression = scriptExpressionFactory.createScriptExpression(script, outputDefinition, expressionProfile, expressionFactory, "script",
                    globalResult);
        } catch (ExpressionSyntaxException | SecurityViolationException e) {
            throw new ScriptExecutionException("Couldn't parse script expression: " + e.getMessage(), e);
        }

        PipelineData output = PipelineData.createEmpty();

        if (forWholeInput) {
            OperationResult result = operationsHelper.createActionResult(null, this, context, globalResult);
            context.checkTaskStop();
            Throwable exception = null;
            try {
                TypedValue<PipelineData> inputTypedValue = new TypedValue<>(input, PipelineData.class);
                Object outObject = executeScript(scriptExpression, inputTypedValue, context.getInitialVariables(), context, result);
                if (outObject != null) {
                    addToData(outObject, PipelineData.newOperationResult(), output);
                } else {
                    // no definition means we don't plan to provide any output - so let's just copy the input item to the output
                    // (actually, null definition with non-null outObject should not occur)
                    if (outputDefinition == null) {
                        output.addAllFrom(input);
                    }
                }
            } catch (Throwable ex) {
                exception = processActionException(ex, NAME, null, context);        // TODO value for error reporting (3rd parameter)
            }
            if (!quiet) {
                context.println((exception != null ? "Attempted to execute " : "Executed ")
                        + "script on the pipeline" + exceptionSuffix(exception));
            }
            operationsHelper.trimAndCloneResult(result, globalResult, context);
        } else {
            for (PipelineItem item : input.getData()) {
                PrismValue value = item.getValue();
                OperationResult result = operationsHelper.createActionResult(item, this, context, globalResult);

                context.checkTaskStop();
                String valueDescription;
                long started;
                if (value instanceof PrismObjectValue) {
                    started = operationsHelper.recordStart(context, asObjectType(value));
                    valueDescription = asObjectType(value).asPrismObject().toString();
                } else {
                    started = 0;
                    valueDescription = value.toHumanReadableString();
                }
                Throwable exception = null;
                try {
                    // Hack. TODO: we need to add definitions to Pipeline items.
                    TypedValue typedValue = new TypedValue(value, value == null ? Object.class : value.getClass());
                    Object outObject = executeScript(scriptExpression, typedValue, item.getVariables(), context, result);
                    if (outObject != null) {
                        addToData(outObject, item.getResult(), output);
                    } else {
                        // no definition means we don't plan to provide any output - so let's just copy the input item to the output
                        // (actually, null definition with non-null outObject should not occur)
                        if (outputDefinition == null) {
                            output.add(item);
                        }
                    }
                    if (value instanceof PrismObjectValue) {
                        operationsHelper.recordEnd(context, asObjectType(value), started, null);
                    }
                } catch (Throwable ex) {
                    if (value instanceof PrismObjectValue) {
                        operationsHelper.recordEnd(context, asObjectType(value), started, ex);
                    }
                    exception = processActionException(ex, NAME, value, context);
                }
                if (!quiet) {
                    context.println((exception != null ? "Attempted to execute " : "Executed ")
                            + "script on " + valueDescription + exceptionSuffix(exception));
                }
                operationsHelper.trimAndCloneResult(result, globalResult, context);
            }
        }
        return output;
    }

    private void addToData(@NotNull Object outObject, @NotNull OperationResult result, PipelineData output) throws SchemaException {
        if (outObject instanceof Collection) {
            for (Object o : (Collection) outObject) {
                addToData(o, result, output);
            }
        } else {
            PrismValue value;
            if (outObject instanceof PrismValue) {
                value = (PrismValue) outObject;
            } else if (outObject instanceof Objectable) {
                value = prismContext.itemFactory().createObjectValue((Objectable) outObject);
            } else if (outObject instanceof Containerable) {
                value = prismContext.itemFactory().createContainerValue((Containerable) outObject);
            } else {
                value = prismContext.itemFactory().createPropertyValue(outObject);
            }
            output.add(new PipelineItem(value, result));
        }
    }

    private ItemDefinition<?> getItemDefinition(String itemUri) throws ScriptExecutionException {
        if (StringUtils.isBlank(itemUri)) {
            return null;
        }

        QName itemName = QNameUtil.uriToQName(itemUri, true);
        ItemDefinition def = prismContext.getSchemaRegistry().findItemDefinitionByElementName(itemName);
        if (def != null) {
            return def;
        }
        def = prismContext.getSchemaRegistry().findItemDefinitionByType(itemName);
        if (def != null) {
            return def;
        }
        throw new ScriptExecutionException("Supplied item identification " + itemUri + " corresponds neither to item name nor type name");
    }

    private <I> Object executeScript(ScriptExpression scriptExpression, TypedValue<I> inputTypedValue,
            VariablesMap externalVariables, ExecutionContext context, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
        ExpressionVariables variables = new ExpressionVariables();
        variables.put(ExpressionConstants.VAR_INPUT, inputTypedValue);
        variables.put(ExpressionConstants.VAR_PRISM_CONTEXT, prismContext, PrismContext.class);
        ExpressionUtil.addActorVariable(variables, securityContextManager, prismContext);
        externalVariables.forEach((k, v) -> variables.put(k, cloneIfNecessary(k, v)));
        variables.registerAliasesFrom(externalVariables);

        List<?> rv = ModelImplUtils.evaluateScript(scriptExpression, null, variables, true, "in '"+NAME+"' action", context.getTask(), result);

        if (rv == null || rv.size() == 0) {
            return null;
        } else if (rv.size() == 1) {
            return rv.get(0);
        } else {
            return rv;        // shouldn't occur; would cause problems
        }
    }

    private ObjectType asObjectType(PrismValue value) {
        return (ObjectType) ((PrismObjectValue) value).asObjectable();
    }

}
