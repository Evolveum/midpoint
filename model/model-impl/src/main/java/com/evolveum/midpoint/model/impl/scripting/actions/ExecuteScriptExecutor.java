/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.scripting.actions;

import java.util.Collection;
import java.util.List;
import jakarta.annotation.PostConstruct;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.PipelineItem;
import com.evolveum.midpoint.model.common.expression.script.ScriptExpression;
import com.evolveum.midpoint.model.common.expression.script.ScriptExpressionFactory;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.model.impl.scripting.PipelineData;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionSyntaxException;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.expression.TypedValue;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionExpressionType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ExecuteScriptActionExpressionType;

/**
 * Executes "execute-script" (s:execute) actions.
 */
@Component
public class ExecuteScriptExecutor extends BaseActionExecutor {

    @Autowired private ScriptExpressionFactory scriptExpressionFactory;
    @Autowired private ExpressionFactory expressionFactory;

    private static final String NAME = "execute-script";
    private static final String PARAM_SCRIPT = "script";
    private static final String PARAM_QUIET = "quiet"; // could be useful for other actions as well
    private static final String PARAM_OUTPUT_ITEM = "outputItem"; // item name or type (as URI!) -- EXPERIMENTAL
    private static final String PARAM_FOR_WHOLE_INPUT = "forWholeInput";

    @PostConstruct
    public void init() {
        actionExecutorRegistry.register(NAME, ExecuteScriptActionExpressionType.class, this);
    }

    private static class Parameters {
        @NotNull private final ScriptExpression scriptExpression;
        private final ItemDefinition<?> outputDefinition;
        private final boolean forWholeInput;
        private final boolean quiet;

        private Parameters(@NotNull ScriptExpression scriptExpression,
                ItemDefinition<?> outputDefinition, boolean forWholeInput, boolean quiet) {
            this.scriptExpression = scriptExpression;
            this.outputDefinition = outputDefinition;
            this.forWholeInput = forWholeInput;
            this.quiet = quiet;
        }
    }

    @Override
    public PipelineData execute(ActionExpressionType action, PipelineData input, ExecutionContext context,
            OperationResult globalResult) throws ScriptExecutionException, SchemaException, ConfigurationException,
            ObjectNotFoundException, CommunicationException, SecurityViolationException, ExpressionEvaluationException {

        checkRootAuthorization(context, globalResult, NAME);

        Parameters parameters = getParameters(action, input, context, globalResult);
        PipelineData output = PipelineData.createEmpty();

        if (parameters.forWholeInput) {
            executeForWholeInput(input, output, parameters, context, globalResult);
        } else {
            iterateOverItems(input, context, globalResult,
                    (value, item, result) ->
                            processItem(context, parameters, output, item, value, result),
                    (value, exception) ->
                            context.println("Failed to execute script on " + getDescription(value) + exceptionSuffix(exception)));
        }
        return output;
    }

    private void executeForWholeInput(PipelineData input, PipelineData output, Parameters parameters, ExecutionContext context,
            OperationResult globalResult) throws ScriptExecutionException {
        OperationResult result = operationsHelper.createActionResult(null, this, globalResult);
        context.checkTaskStop();
        try {
            TypedValue<PipelineData> inputTypedValue = new TypedValue<>(input, PipelineData.class);
            Object outObject = executeScript(parameters.scriptExpression, inputTypedValue, context.getInitialVariables(), context, result);
            if (outObject != null) {
                addToData(outObject, PipelineData.newOperationResult(), output);
            } else {
                // no definition means we don't plan to provide any output - so let's just copy the input item to the output
                // (actually, null definition with non-null outObject should not occur)
                if (parameters.outputDefinition == null) {
                    output.addAllFrom(input);
                }
            }
            if (!parameters.quiet) {
                context.println("Executed script on the pipeline");
            }

        } catch (Throwable ex) {
            Throwable exception = processActionException(ex, NAME, null, context); // TODO value for error reporting (3rd parameter)
            context.println("Failed to execute script on the pipeline" + exceptionSuffix(exception));
        }
        operationsHelper.trimAndCloneResult(result, null);
    }

    private void processItem(ExecutionContext context, Parameters parameters,
            PipelineData output, PipelineItem item, PrismValue value, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        // Hack. TODO: we need to add definitions to Pipeline items.
        @SuppressWarnings({ "unchecked", "rawtypes" })
        TypedValue<?> typedValue = new TypedValue(value, value == null ? Object.class : value.getClass());
        Object outObject = executeScript(parameters.scriptExpression, typedValue, item.getVariables(), context, result);
        if (outObject != null) {
            addToData(outObject, item.getResult(), output);
        } else {
            // no definition means we don't plan to provide any output - so let's just copy the input item to the output
            // (actually, null definition with non-null outObject should not occur)
            if (parameters.outputDefinition == null) {
                output.add(item);
            }
        }
        if (!parameters.quiet) {
            context.println("Executed script on " + getDescription(value));
        }
    }

    private Parameters getParameters(ActionExpressionType action, PipelineData input, ExecutionContext context,
            OperationResult globalResult) throws CommunicationException, ObjectNotFoundException, SchemaException,
            ScriptExecutionException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        ScriptExpressionEvaluatorType script = expressionHelper.getActionArgument(ScriptExpressionEvaluatorType.class, action,
                ExecuteScriptActionExpressionType.F_SCRIPT, PARAM_SCRIPT, input, context, null, PARAM_SCRIPT, globalResult);
        ItemDefinition<?> outputDefinition;
        String outputItemUri = expressionHelper.getSingleArgumentValue(action.getParameter(), PARAM_OUTPUT_ITEM, false, false,
                NAME, input, context, String.class, globalResult);
        if (StringUtils.isNotBlank(outputItemUri)) {
            outputDefinition = getItemDefinition(outputItemUri);
        } else if (action instanceof ExecuteScriptActionExpressionType) {
            ExecuteScriptActionExpressionType execute = (ExecuteScriptActionExpressionType) action;
            if (execute.getOutputItemName() != null) {
                outputDefinition = getItemDefinitionFromItemName(execute.getOutputItemName());
            } else if (execute.getOutputTypeName() != null) {
                outputDefinition = getItemDefinitionFromTypeName(execute.getOutputTypeName());
            } else {
                outputDefinition = null;
            }
        } else {
            outputDefinition = null;
        }
        boolean forWholeInput = expressionHelper.getActionArgument(Boolean.class, action,
                ExecuteScriptActionExpressionType.F_FOR_WHOLE_INPUT, PARAM_FOR_WHOLE_INPUT, input, context, false, PARAM_FOR_WHOLE_INPUT, globalResult);
        boolean quiet = expressionHelper.getActionArgument(Boolean.class, action,
                ExecuteScriptActionExpressionType.F_QUIET, PARAM_QUIET, input, context, false, PARAM_QUIET, globalResult);

        if (script == null) {
            throw new IllegalArgumentException("No script provided");
        }

        ExpressionProfile expressionProfile = null; // TODO

        ScriptExpression scriptExpression;
        try {
            scriptExpression =
                    scriptExpressionFactory.createScriptExpression(
                            script, outputDefinition, expressionProfile, "script", globalResult);
        } catch (ExpressionSyntaxException | SecurityViolationException e) {
            throw new ScriptExecutionException("Couldn't parse script expression: " + e.getMessage(), e);
        }

        return new Parameters(scriptExpression, outputDefinition, forWholeInput, quiet);
    }

    private void addToData(@NotNull Object outObject, @NotNull OperationResult result, PipelineData output) {
        if (outObject instanceof Collection) {
            for (Object o : (Collection<?>) outObject) {
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

    private ItemDefinition<?> getItemDefinition(String uri) throws ScriptExecutionException {
        QName name = QNameUtil.uriToQName(uri, true);
        ItemDefinition<?> byName = prismContext.getSchemaRegistry().findItemDefinitionByElementName(name);
        if (byName != null) {
            return byName;
        }

        ItemDefinition<?> byType = prismContext.getSchemaRegistry().findItemDefinitionByType(name);
        if (byType != null) {
            return byType;
        }

        throw new ScriptExecutionException("Supplied item identification '" + uri + "' corresponds neither to item name nor type name");
    }

    private ItemDefinition<?> getItemDefinitionFromItemName(QName itemName) throws ScriptExecutionException {
        ItemDefinition<?> def = prismContext.getSchemaRegistry().findItemDefinitionByElementName(itemName);
        if (def != null) {
            return def;
        }
        throw new ScriptExecutionException("Item with name '" + itemName + "' couldn't be found.");
    }

    private ItemDefinition<?> getItemDefinitionFromTypeName(QName typeName) throws ScriptExecutionException {
        ItemDefinition<?> byType = prismContext.getSchemaRegistry().findItemDefinitionByType(typeName);
        if (byType != null) {
            return byType;
        }

        if (XmlTypeConverter.canConvert(typeName)) {
            return prismContext.definitionFactory().createPropertyDefinition(SchemaConstantsGenerated.C_VALUE, typeName);
        }

        TypeDefinition typeDef = prismContext.getSchemaRegistry().findTypeDefinitionByType(typeName);
        if (typeDef instanceof SimpleTypeDefinition) {
            return prismContext.definitionFactory().createPropertyDefinition(SchemaConstantsGenerated.C_VALUE, typeName);
        } else if (typeDef instanceof ComplexTypeDefinition) {
            ComplexTypeDefinition ctd = (ComplexTypeDefinition) typeDef;
            if (ctd.isContainerMarker() || ctd.isObjectMarker()) {
                return prismContext.definitionFactory().createContainerDefinition(SchemaConstantsGenerated.C_VALUE, ctd);
            } else {
                return prismContext.definitionFactory().createPropertyDefinition(SchemaConstantsGenerated.C_VALUE, typeName);
            }
        } else if (typeDef != null) {
            throw new ScriptExecutionException("Type with name '" + typeName + "' couldn't be used as output type: " + typeDef);
        } else {
            throw new ScriptExecutionException("Type with name '" + typeName + "' couldn't be found.");
        }
    }

    private <I> Object executeScript(ScriptExpression scriptExpression, TypedValue<I> inputTypedValue,
            VariablesMap externalVariables, ExecutionContext context, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
        VariablesMap variables = createVariables(externalVariables);

        variables.put(ExpressionConstants.VAR_INPUT, inputTypedValue);

        LensContext<?> lensContext = getLensContext(externalVariables);
        List<?> rv = ModelImplUtils.evaluateScript(
                scriptExpression, lensContext, variables, true,
                "in '" + NAME + "' action", context.getTask(), result);

        if (rv.isEmpty()) {
            return null;
        } else if (rv.size() == 1) {
            return rv.get(0);
        } else {
            return rv; // shouldn't occur; would cause problems
        }
    }

    // TODO implement seriously! This implementation requires custom modelContext variable that might or might not be present
    //  (it is set e.g. for policy rule script execution)
    private LensContext<?> getLensContext(VariablesMap externalVariables) {
        TypedValue<?> modelContextTypedValue = externalVariables.get(ExpressionConstants.VAR_MODEL_CONTEXT);
        return modelContextTypedValue != null ? (LensContext<?>) modelContextTypedValue.getValue() : null;
    }

    @Override
    String getActionName() {
        return NAME;
    }
}
