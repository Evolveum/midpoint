/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.scripting.actions;

import java.util.Collection;
import java.util.function.Function;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionExpressionType;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.PipelineItem;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.model.impl.scripting.PipelineData;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.TypedValue;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.AbstractExecuteActionExpressionType;

/**
 * Executes either `execute-script` or `evaluate-expression` actions.
 */
abstract class AbstractExecuteExecutor<P extends AbstractExecuteExecutor.Parameters>
        extends BaseActionExecutor {

    private static final String PARAM_QUIET = "quiet"; // could be useful for other actions as well
    private static final String PARAM_OUTPUT_ITEM = "outputItem"; // item name or type (as URI!) -- EXPERIMENTAL
    private static final String PARAM_FOR_WHOLE_INPUT = "forWholeInput";

    P getParameters(
            ActionExpressionType action, PipelineData input, ExecutionContext context,
            OperationResult globalResult, Function<Parameters, P> function)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException, SecurityViolationException,
            PolicyViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {

        ItemDefinition<?> outputDefinition;
        String outputItemUri = expressionHelper.getSingleArgumentValue(
                action.getParameter(), PARAM_OUTPUT_ITEM, false, false,
                getName(), input, context, String.class, globalResult);
        if (StringUtils.isNotBlank(outputItemUri)) {
            outputDefinition = getItemDefinition(outputItemUri);
        } else if (action instanceof AbstractExecuteActionExpressionType execute) {
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
        boolean forWholeInput = expressionHelper.getActionArgument(
                Boolean.class, action,
                AbstractExecuteActionExpressionType.F_FOR_WHOLE_INPUT, PARAM_FOR_WHOLE_INPUT,
                input, context, false, PARAM_FOR_WHOLE_INPUT, globalResult);
        boolean quiet = expressionHelper.getActionArgument(
                Boolean.class, action,
                AbstractExecuteActionExpressionType.F_QUIET, PARAM_QUIET,
                input, context, false, PARAM_QUIET, globalResult);

        return function.apply(new Parameters(outputDefinition, forWholeInput, quiet));
    }

    private @NotNull ItemDefinition<?> getItemDefinition(String uri) throws ConfigurationException {
        QName name = QNameUtil.uriToQName(uri, true);
        ItemDefinition<?> byName = prismContext.getSchemaRegistry().findItemDefinitionByElementName(name);
        if (byName != null) {
            return byName;
        }

        ItemDefinition<?> byType = prismContext.getSchemaRegistry().findItemDefinitionByType(name);
        if (byType != null) {
            return byType;
        }

        throw new ConfigurationException(
                "Supplied item identification '" + uri + "' corresponds neither to item name nor type name");
    }

    private @NotNull ItemDefinition<?> getItemDefinitionFromItemName(QName itemName) throws ConfigurationException {
        ItemDefinition<?> def = prismContext.getSchemaRegistry().findItemDefinitionByElementName(itemName);
        if (def != null) {
            return def;
        }
        throw new ConfigurationException("Item with name '" + itemName + "' couldn't be found.");
    }

    private @NotNull ItemDefinition<?> getItemDefinitionFromTypeName(QName typeName) throws ConfigurationException {
        ItemDefinition<?> byType = prismContext.getSchemaRegistry().findItemDefinitionByType(typeName);
        if (byType != null) {
            return byType;
        }

        if (XmlTypeConverter.canConvert(typeName)) {
            return prismContext.definitionFactory().newPropertyDefinition(SchemaConstantsGenerated.C_VALUE, typeName);
        }

        TypeDefinition typeDef = prismContext.getSchemaRegistry().findTypeDefinitionByType(typeName);
        if (typeDef instanceof SimpleTypeDefinition) {
            return prismContext.definitionFactory().newPropertyDefinition(SchemaConstantsGenerated.C_VALUE, typeName);
        } else if (typeDef instanceof ComplexTypeDefinition ctd) {
            if (ctd.isContainerMarker() || ctd.isObjectMarker()) {
                return prismContext.definitionFactory().newContainerDefinition(SchemaConstantsGenerated.C_VALUE, ctd);
            } else {
                return prismContext.definitionFactory().newPropertyDefinition(SchemaConstantsGenerated.C_VALUE, typeName);
            }
        } else if (typeDef != null) {
            throw new ConfigurationException("Type with name '" + typeName + "' couldn't be used as output type: " + typeDef);
        } else {
            throw new ConfigurationException("Type with name '" + typeName + "' couldn't be found.");
        }
    }


    @NotNull PipelineData executeInternal(
            PipelineData input, P parameters, ExecutionContext context, OperationResult globalResult)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, PolicyViolationException,
            CommunicationException, ConfigurationException, ObjectNotFoundException, ObjectAlreadyExistsException {
        PipelineData output = PipelineData.createEmpty();
        if (parameters.forWholeInput) {
            executeForWholeInput(input, output, parameters, context, globalResult);
        } else {
            iterateOverItems(input, context, globalResult,
                    (value, item, result) ->
                            processItem(context, parameters, output, item, value, result),
                    (value, exception) ->
                            context.println("Failed to execute script/expression on "
                                    + getDescription(value) + exceptionSuffix(exception)));
        }
        return output;
    }

    private void executeForWholeInput(
            PipelineData input, PipelineData output, P parameters, ExecutionContext context, OperationResult globalResult)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException, PolicyViolationException, ObjectAlreadyExistsException {
        context.checkTaskStop();
        OperationResult result = operationsHelper.createActionResult(null, this, globalResult);
        try {
            TypedValue<PipelineData> inputTypedValue = new TypedValue<>(input, PipelineData.class);
            Object outObject = doSingleExecution(parameters, inputTypedValue, context.getInitialVariables(), context, result);
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
                context.println("Executed script/expression on the pipeline");
            }
        } catch (Throwable ex) {
            result.recordException(ex);
            Throwable exception = logOrRethrowActionException(ex, null, context); // TODO value for error reporting (2nd parameter)
            context.println("Failed to execute script/expression on the pipeline" + exceptionSuffix(exception));
        } finally {
            result.close();
        }
        operationsHelper.trimAndCloneResult(result, null);
    }

    private void processItem(
            ExecutionContext context, P parameters,
            PipelineData output, PipelineItem item, PrismValue value, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException {

        // Hack. TODO: we need to add definitions to Pipeline items.
        @SuppressWarnings({ "unchecked", "rawtypes" })
        TypedValue<?> typedValue = new TypedValue(value, value == null ? Object.class : value.getClass());
        Object outObject = doSingleExecution(parameters, typedValue, item.getVariables(), context, result);
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
            context.println("Executed script/expression on " + getDescription(value));
        }
    }

    abstract <I> Object doSingleExecution(P parameters, TypedValue<I> inputTypedValue,
            VariablesMap externalVariables, ExecutionContext context, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException;

    private void addToData(@NotNull Object outObject, @NotNull OperationResult result, PipelineData output) {
        if (outObject instanceof Collection<?> objects) {
            for (Object o : objects) {
                addToData(o, result, output);
            }
        } else {
            PrismValue value;
            if (outObject instanceof PrismValue prismValue) {
                value = prismValue;
            } else if (outObject instanceof Containerable containerable) {
                value = containerable.asPrismContainerValue();
            } else {
                value = prismContext.itemFactory().createPropertyValue(outObject);
            }
            output.add(new PipelineItem(value, result));
        }
    }

    // TODO implement seriously! This implementation requires custom modelContext variable that might or might not be present
    //  (it is set e.g. for policy rule script execution)
    <F extends ObjectType> LensContext<F> getLensContext(VariablesMap externalVariables) {
        TypedValue<?> modelContextTypedValue = externalVariables.get(ExpressionConstants.VAR_MODEL_CONTEXT);
        //noinspection unchecked
        return modelContextTypedValue != null ? (LensContext<F>) modelContextTypedValue.getValue() : null;
    }

    /**
     * Parameters for `execute-script` and `evaluate-expression` actions.
     */
    static class Parameters {

        final ItemDefinition<?> outputDefinition;
        final boolean forWholeInput;
        final boolean quiet;

        Parameters(
                ItemDefinition<?> outputDefinition,
                boolean forWholeInput,
                boolean quiet) {
            this.outputDefinition = outputDefinition;
            this.forWholeInput = forWholeInput;
            this.quiet = quiet;
        }
    }
}
