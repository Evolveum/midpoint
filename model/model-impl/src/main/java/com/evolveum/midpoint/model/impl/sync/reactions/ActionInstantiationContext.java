/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.reactions;

import com.evolveum.midpoint.model.impl.sync.SynchronizationContext;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.schema.processor.SynchronizationActionDefinition;
import com.evolveum.midpoint.schema.processor.SynchronizationReactionDefinition;

import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

import org.jetbrains.annotations.NotNull;

/**
 * Contains various objects needed for the instantiation of a synchronization action.
 * (They are bundled together here to make action instantiation simpler.)
 */
public class ActionInstantiationContext<F extends FocusType> {

    @NotNull public final SynchronizationContext.Complete<F> syncCtx;
    @NotNull public final ResourceObjectShadowChangeDescription change;
    @NotNull public final SynchronizationReactionDefinition reactionDefinition;
    @NotNull public final SynchronizationActionDefinition actionDefinition;

    public ActionInstantiationContext(
            @NotNull SynchronizationContext.Complete<F> syncCtx,
            @NotNull ResourceObjectShadowChangeDescription change,
            @NotNull SynchronizationReactionDefinition reactionDefinition,
            @NotNull SynchronizationActionDefinition actionDefinition) {
        this.syncCtx = syncCtx;
        this.change = change;
        this.reactionDefinition = reactionDefinition;
        this.actionDefinition = actionDefinition;
    }
}
