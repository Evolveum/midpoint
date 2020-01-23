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
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

/**
 * @author semancik
 *
 */
public class LinkAction implements Action {

    @Override
    public <F extends FocusType> void handle(LensContext<F> context, SynchronizationSituation<F> situation,
            Map<QName, Object> parameters, Task task, OperationResult parentResult) {

        // Just add the candidate focus to the context. It will be linked in
        // synchronization.

        F focusType = situation.getCorrelatedOwner();
        LensFocusContext<F> focusContext = context.createFocusContext();
        PrismObject<F> focusOld = (PrismObject<F>) focusType.asPrismObject();
        focusContext.setLoadedObject(focusOld);

    }

}
