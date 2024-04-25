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

import com.evolveum.midpoint.xml.ns._public.common.common_3.DeleteFocusSynchronizationActionType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

/**
 * @author semancik
 */
@ActionUris({
        "http://midpoint.evolveum.com/xml/ns/public/model/action-3#deleteFocus",
        "http://midpoint.evolveum.com/xml/ns/public/model/action-3#deleteUser" })
@ActionDefinitionClass(DeleteFocusSynchronizationActionType.class)
class DeleteFocusAction<F extends FocusType> extends BaseClockworkAction<F> {

    private static final Trace LOGGER = TraceManager.getTrace(DeleteFocusAction.class);

    DeleteFocusAction(@NotNull ActionInstantiationContext<F> ctx) {
        super(ctx);
    }

    @Override
    public void prepareContext(@NotNull LensContext<F> context, @NotNull OperationResult result) {
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
