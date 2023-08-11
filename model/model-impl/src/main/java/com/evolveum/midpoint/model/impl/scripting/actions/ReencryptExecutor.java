/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.scripting.actions;

import com.evolveum.midpoint.common.crypto.CryptoUtil;
import com.evolveum.midpoint.model.api.PipelineItem;
import com.evolveum.midpoint.schema.statistics.Operation;
import com.evolveum.midpoint.util.exception.ScriptExecutionException;
import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.model.impl.scripting.PipelineData;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionExpressionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Collection;

/**
 * Executes "reencrypt" action.
 *
 * There is no static (typed) definition of this action yet.
 * Also, this code is not refactored yet.
 */
@Component
public class ReencryptExecutor extends BaseActionExecutor {

    //private static final Trace LOGGER = TraceManager.getTrace(ReencryptExecutor.class);

    private static final String NAME = "reencrypt";

    @PostConstruct
    public void init() {
        actionExecutorRegistry.register(NAME, this);
    }

    @Override
    public PipelineData execute(ActionExpressionType expression, PipelineData input, ExecutionContext context, OperationResult globalResult) throws ScriptExecutionException, SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, SecurityViolationException, ExpressionEvaluationException {

        Protector protector = prismContext.getDefaultProtector();

        boolean dryRun = operationsHelper.getDryRun(expression, input, context, globalResult);

        PipelineData output = PipelineData.createEmpty();

        for (PipelineItem item: input.getData()) {
            PrismValue value = item.getValue();
            OperationResult result = operationsHelper.createActionResult(item, this, globalResult);
            context.checkTaskStop();
            if (value instanceof PrismObjectValue) {
                @SuppressWarnings({"unchecked", "raw"})
                PrismObject<? extends ObjectType> prismObject = ((PrismObjectValue) value).asPrismObject();
                ObjectType objectBean = prismObject.asObjectable();
                Operation op = operationsHelper.recordStart(context, objectBean);
                try {
                    Collection<? extends ItemDelta<?, ?>> modifications = CryptoUtil.computeReencryptModifications(protector, prismObject);
                    if (!modifications.isEmpty()) {
                        result.addArbitraryObjectCollectionAsParam("modifications", modifications);
                        if (dryRun) {
                            context.println("Would reencrypt (this is dry run) " + prismObject.toString() + ": " + modifications.size() + " modification(s)");
                        } else {
                            cacheRepositoryService.modifyObject(objectBean.getClass(), objectBean.getOid(), modifications, result);
                            context.println("Reencrypted " + prismObject + ": " + modifications.size() + " modification(s)");
                        }
                    }
                    result.computeStatus();
                    operationsHelper.recordEnd(context, op, null, result);
                } catch (Throwable ex) {
                    result.recordFatalError("Couldn't reencrypt object", ex);
                    operationsHelper.recordEnd(context, op, ex, result);
                    Throwable exception = processActionException(ex, NAME, value, context);
                    context.println("Couldn't reencrypt " + prismObject.toString() + drySuffix(dryRun) + exceptionSuffix(exception));
                }
                PrismPropertyValue<String> oidVal = prismContext.itemFactory().createPropertyValue(objectBean.getOid());
                output.add(new PipelineItem(oidVal, item.getResult()));
            } else {
                //noinspection ThrowableNotThrown
                processActionException(new ScriptExecutionException("Item is not a PrismObject"), NAME, value, context);
            }
            operationsHelper.trimAndCloneResult(result, item.getResult());
        }
        return output;
    }

    @Override
    @NotNull String getLegacyActionName() {
        return NAME;
    }

    @Override
    @Nullable String getConfigurationElementName() {
        return null;
    }
}
