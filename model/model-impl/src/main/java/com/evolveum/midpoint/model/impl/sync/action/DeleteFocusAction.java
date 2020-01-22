/*
 * Copyright (c) 2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.sync.action;

import java.util.Map;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.sync.Action;
import com.evolveum.midpoint.model.impl.sync.SynchronizationSituation;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

/**
 * @author semancik
 *
 */
public class DeleteFocusAction implements Action {

    private static final Trace LOGGER = TraceManager.getTrace(DeleteFocusAction.class);

    /* (non-Javadoc)
     * @see com.evolveum.midpoint.model.sync.Action#handle(com.evolveum.midpoint.model.lens.LensContext, com.evolveum.midpoint.model.sync.SynchronizationSituation, java.util.Map, com.evolveum.midpoint.task.api.Task, com.evolveum.midpoint.schema.result.OperationResult)
     */
    @Override
    public <F extends FocusType> void handle(LensContext<F> context, SynchronizationSituation<F> situation,
            Map<QName, Object> parameters, Task task, OperationResult parentResult) {

        LensFocusContext<F> focusContext = context.getFocusContext();
        if (focusContext != null) {
            PrismObject<F> objectOld = focusContext.getObjectOld();
            ObjectDelta<F> delta = objectOld.createDeleteDelta();
            focusContext.setPrimaryDelta(delta);
            LOGGER.trace("Setting primary delta to focus context: {}", delta);
        } else {
            LOGGER.trace("No focus context");
        }

    }

}
