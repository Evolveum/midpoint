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

import com.evolveum.midpoint.xml.ns._public.common.common_3.LinkSynchronizationActionType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType.UNLINKED;

/**
 * @author semancik
 *
 */
@ActionUris({
        "http://midpoint.evolveum.com/xml/ns/public/model/action-3#link",
        "http://midpoint.evolveum.com/xml/ns/public/model/action-3#linkAccount" })
@ActionDefinitionClass(LinkSynchronizationActionType.class)
public class LinkAction<F extends FocusType> extends BaseClockworkAction<F> {

    public LinkAction(@NotNull ActionInstantiationContext<F> ctx) {
        super(ctx);
    }

    @Override
    public void prepareContext(@NotNull LensContext<F> context, @NotNull OperationResult result) {

        // Just add the candidate focus to the context. It will be linked in synchronization.
        F focus = syncCtx.getCorrelatedOwner();

        if (focus == null) {
            String problem = "Couldn't invoke the 'link' action because there is no correlated owner.";
            SynchronizationSituationType situation = syncCtx.getSituation();
            if (situation != UNLINKED) {
                throw new IllegalStateException(problem + " This action is supported only for UNLINKED situation. "
                        + "In this case the situation is " + situation + ".");
            } else {
                throw new IllegalStateException(problem);
            }
        }
        LensFocusContext<F> focusContext = context.createFocusContext();
        //noinspection unchecked
        focusContext.setInitialObject((PrismObject<F>) focus.asPrismObject());
    }
}
