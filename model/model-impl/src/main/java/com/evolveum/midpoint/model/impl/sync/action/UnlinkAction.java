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
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.api.context.SynchronizationIntent;
import com.evolveum.midpoint.model.impl.sync.Action;
import com.evolveum.midpoint.model.impl.sync.SynchronizationSituation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import org.jetbrains.annotations.NotNull;

/**
 * @author semancik
 */
public class UnlinkAction implements Action {

    @Override
    public <F extends FocusType> void handle(@NotNull LensContext<F> context, SynchronizationSituation<F> situation,
            Map<QName, Object> parameters, Task task, OperationResult parentResult) {

        // This is a no-op (temporary). In fact, we do not want the link to be removed.
        // Unlink is used as a default reaction to DELETE situation, meaning "make link inactive".
        // Since 4.3 this is modeled by setting link relation as org:related (dead links).

//        LensProjectionContext projectionContext = context.getProjectionContextsIterator().next();
//        projectionContext.setSynchronizationIntent(SynchronizationIntent.UNLINK);
    }
}
