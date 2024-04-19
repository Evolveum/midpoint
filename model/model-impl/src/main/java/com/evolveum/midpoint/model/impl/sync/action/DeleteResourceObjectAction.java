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

import com.evolveum.midpoint.xml.ns._public.common.common_3.DeleteResourceObjectSynchronizationActionType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.context.SynchronizationIntent;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

/**
 * @author semancik
 *
 */
@ActionUris({
        "http://midpoint.evolveum.com/xml/ns/public/model/action-3#deleteShadow",
        "http://midpoint.evolveum.com/xml/ns/public/model/action-3#deleteAccount",
        "http://midpoint.evolveum.com/xml/ns/public/model/action-3#deleteResourceObject" // not officially supported
})
@ActionDefinitionClass(DeleteResourceObjectSynchronizationActionType.class)
public class DeleteResourceObjectAction<F extends FocusType> extends BaseClockworkAction<F> {

    DeleteResourceObjectAction(@NotNull ActionInstantiationContext<F> ctx) {
        super(ctx);
    }

    @Override
    public void prepareContext(@NotNull LensContext<F> context, @NotNull OperationResult result) {
        LensProjectionContext projectionContext = context.getProjectionContextsIterator().next();
        projectionContext.setSynchronizationIntent(SynchronizationIntent.DELETE);
    }
}
