/*
 * Copyright (c) 2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.sync.action;

import com.evolveum.midpoint.model.impl.sync.reactions.ActionDefinitionClass;
import com.evolveum.midpoint.model.impl.sync.reactions.ActionInstantiationContext;

import com.evolveum.midpoint.model.impl.sync.reactions.ActionUris;

import com.evolveum.midpoint.xml.ns._public.common.common_3.UnlinkSynchronizationActionType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

/**
 * @author semancik
 */
@ActionUris({
        "http://midpoint.evolveum.com/xml/ns/public/model/action-3#unlink",
        "http://midpoint.evolveum.com/xml/ns/public/model/action-3#unlinkAccount" })
@ActionDefinitionClass(UnlinkSynchronizationActionType.class)
public class UnlinkAction<F extends FocusType> extends BaseClockworkAction<F> {

    UnlinkAction(@NotNull ActionInstantiationContext<F> ctx) {
        super(ctx);
    }

    @Override
    public void prepareContext(@NotNull LensContext<F> context, @NotNull OperationResult result) {

        // This is a no-op (temporary). In fact, we do not want the link to be removed.
        // Unlink is used as a default reaction to DELETE situation, meaning "make link inactive".
        // Since 4.3 this is modeled by setting link relation as org:related (dead links).

//        LensProjectionContext projectionContext = context.getProjectionContextsIterator().next();
//        projectionContext.setSynchronizationIntent(SynchronizationIntent.UNLINK);
    }
}
