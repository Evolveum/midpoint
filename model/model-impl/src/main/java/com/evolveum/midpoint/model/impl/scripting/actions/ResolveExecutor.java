/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.scripting.actions;

import java.util.Collection;
import jakarta.annotation.PostConstruct;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.util.GetOperationOptionsUtil;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.PipelineItem;
import com.evolveum.midpoint.util.exception.ScriptExecutionException;
import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.model.impl.scripting.PipelineData;
import com.evolveum.midpoint.prism.PrismObjectValue;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SelectorQualifiedGetOptionsType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionExpressionType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ResolveReferenceActionExpressionType;

/**
 * Resolves a reference, e.g. a linkRef into a set of accounts.
 */
@Component
public class ResolveExecutor extends BaseActionExecutor {

    private static final String NAME = "resolve";
    private static final String PARAM_NO_FETCH = "noFetch";

    @PostConstruct
    public void init() {
        actionExecutorRegistry.register(NAME, ResolveReferenceActionExpressionType.class, this);
    }

    @Override
    public PipelineData execute(ActionExpressionType action, PipelineData input, ExecutionContext context,
            OperationResult globalResult) throws ScriptExecutionException, SchemaException, ConfigurationException,
            ObjectNotFoundException, CommunicationException, SecurityViolationException, ExpressionEvaluationException {

        Collection<SelectorOptions<GetOperationOptions>> options;
        if (action instanceof ResolveReferenceActionExpressionType) {
            SelectorQualifiedGetOptionsType optionsBean = ((ResolveReferenceActionExpressionType) action).getOptions();
            options = GetOperationOptionsUtil.optionsBeanToOptions(optionsBean);
        } else {
            boolean noFetch = expressionHelper.getArgumentAsBoolean(action.getParameter(), PARAM_NO_FETCH, input, context, false, NAME, globalResult);
            options = schemaService.getOperationOptionsBuilder().noFetch(noFetch).build();
        }

        PipelineData output = PipelineData.createEmpty();

        iterateOverItems(input, context, globalResult,
                (value, item, result) ->
                        resolveReference(context, options, output, item, value, result),
                (value, exception) ->
                        context.println("Couldn't resolve reference: " + value + exceptionSuffix(exception)));

        return output;
    }

    private void resolveReference(ExecutionContext context, Collection<SelectorOptions<GetOperationOptions>> options,
            PipelineData output, PipelineItem item, PrismValue value, OperationResult result) throws ScriptExecutionException {
        if (value instanceof PrismReferenceValue) {
            PrismReferenceValue prismReferenceValue = (PrismReferenceValue) value;
            String oid = prismReferenceValue.getOid();
            QName targetTypeQName = prismReferenceValue.getTargetType();
            if (targetTypeQName == null) {
                throw new ScriptExecutionException("Couldn't resolve reference, because target type is unknown: " + prismReferenceValue);
            }
            Class<? extends ObjectType> type = prismContext.getSchemaRegistry().determineCompileTimeClass(targetTypeQName);
            if (type == null) {
                throw new ScriptExecutionException("Couldn't resolve reference, because target type class is unknown for target type " + targetTypeQName);
            }
            try {
                PrismObjectValue<? extends ObjectType> resolved = modelService.getObject(type, oid, options, context.getTask(), result).getValue();
                output.add(new PipelineItem(resolved, item.getResult()));
            } catch (Throwable e) {
                //noinspection ThrowableNotThrown
                processActionException(e, NAME, value, context);
                output.add(item); // to keep track of failed item (may trigger exceptions downstream)
            }
        } else {
            //noinspection ThrowableNotThrown
            processActionException(new ScriptExecutionException("Value is not a reference"), NAME, value, context);
            output.add(item); // to keep track of failed item (may trigger exceptions downstream)
        }
    }

    @Override
    String getActionName() {
        return NAME;
    }
}
