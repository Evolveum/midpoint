/*
 * Copyright (c) 2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.sync.action;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.impl.sync.reactions.ActionDefinitionClass;
import com.evolveum.midpoint.model.impl.sync.reactions.ActionInstantiationContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CreateCorrelationCaseSynchronizationActionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

@ActionDefinitionClass(CreateCorrelationCaseSynchronizationActionType.class)
public class CreateCorrelationCaseAction<F extends FocusType> extends BaseAction<F> {

    private static final Trace LOGGER = TraceManager.getTrace(CreateCorrelationCaseAction.class);

    private static final String OP_HANDLE = CreateCorrelationCaseAction.class.getName() + ".handle";

    public CreateCorrelationCaseAction(@NotNull ActionInstantiationContext<F> ctx) {
        super(ctx);
    }

    @Override
    public void handle(@NotNull OperationResult parentResult) throws CommonException {
        OperationResult result = parentResult.subresult(OP_HANDLE).build();
        try {
            if (syncCtx.isFullMode()) {
                beans.correlationCaseManager.createOrUpdateCase(
                        syncCtx.getShadowedResourceObject(),
                        syncCtx.getResource(),
                        syncCtx.getPreFocus(),
                        syncCtx.getTask(),
                        result);
            } else {
                LOGGER.debug("Not creating nor updating correlation case because not in full execution mode: {}",
                        syncCtx.getExecutionMode());
                result.recordNotApplicable("Not in full execution mode");
            }
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.close();
        }
    }
}
