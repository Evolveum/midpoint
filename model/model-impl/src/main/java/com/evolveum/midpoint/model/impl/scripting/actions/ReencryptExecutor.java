/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.scripting.actions;

import java.util.Collection;

import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionExpressionType;

import jakarta.annotation.PostConstruct;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.crypto.CryptoUtil;
import com.evolveum.midpoint.model.api.BulkAction;
import com.evolveum.midpoint.model.api.PipelineItem;
import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.model.impl.scripting.PipelineData;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectValue;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.Operation;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Executes "reencrypt" action.
 *
 * There is no static (typed) definition of this action yet.
 * Also, this code is not refactored yet.
 */
@Component
public class ReencryptExecutor extends BaseActionExecutor {

    @PostConstruct
    public void init() {
        actionExecutorRegistry.register(this);
    }

    @Override
    public @NotNull BulkAction getActionType() {
        return BulkAction.REENCRYPT;
    }

    @Override
    public PipelineData execute(
            ActionExpressionType action, PipelineData input, ExecutionContext context, OperationResult globalResult)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException, SecurityViolationException,
            PolicyViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {

        Protector protector = prismContext.getDefaultProtector();

        boolean dryRun = operationsHelper.getDryRun(action, input, context, globalResult);

        PipelineData output = PipelineData.createEmpty();

        for (PipelineItem item: input.getData()) {
            PrismValue value = item.getValue();
            context.checkTaskStop();
            OperationResult result = operationsHelper.createActionResult(item, this, globalResult);
            try {
                if (value instanceof PrismObjectValue) {
                    //noinspection unchecked,rawtypes
                    PrismObject<? extends ObjectType> prismObject = ((PrismObjectValue) value).asPrismObject();
                    ObjectType objectBean = prismObject.asObjectable();
                    Operation op = operationsHelper.recordStart(context, objectBean);
                    try {
                        Collection<? extends ItemDelta<?, ?>> modifications =
                                CryptoUtil.computeReencryptModifications(protector, prismObject);
                        if (!modifications.isEmpty()) {
                            result.addArbitraryObjectCollectionAsParam("modifications", modifications);
                            if (dryRun) {
                                context.println("Would reencrypt (this is dry run) %s: %d modification(s)".formatted(
                                        prismObject.toString(), modifications.size()));
                            } else {
                                cacheRepositoryService.modifyObject(
                                        objectBean.getClass(), objectBean.getOid(), modifications, result);
                                context.println(
                                        "Reencrypted %s: %d modification(s)".formatted(
                                                prismObject, modifications.size()));
                            }
                        }
                        result.computeStatus();
                        operationsHelper.recordEnd(context, op, null, result);
                    } catch (Throwable ex) {
                        result.recordFatalError("Couldn't reencrypt object", ex);
                        operationsHelper.recordEnd(context, op, ex, result);
                        Throwable exception = logOrRethrowActionException(ex, value, context);
                        context.println("Couldn't reencrypt " + prismObject + drySuffix(dryRun) + exceptionSuffix(exception));
                    }
                    PrismPropertyValue<String> oidVal = prismContext.itemFactory().createPropertyValue(objectBean.getOid());
                    output.add(new PipelineItem(oidVal, item.getResult()));
                } else {
                    //noinspection ThrowableNotThrown
                    logOrRethrowActionException(new UnsupportedOperationException("Item is not a PrismObject"), value, context);
                }
            } catch (Throwable t) {
                result.recordException(t);
                throw t;
            } finally {
                result.close();
            }
            operationsHelper.trimAndCloneResult(result, item.getResult());
        }
        return output;
    }
}
