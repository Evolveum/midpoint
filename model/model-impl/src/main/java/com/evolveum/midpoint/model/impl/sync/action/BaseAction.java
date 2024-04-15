/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.action;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.sync.SynchronizationContext;
import com.evolveum.midpoint.model.impl.sync.reactions.ActionInstantiationContext;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.schema.processor.SynchronizationActionDefinition;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

/**
 * @author lazyman
 */
public abstract class BaseAction<F extends FocusType> implements SynchronizationAction {

    protected static final Trace LOGGER = TraceManager.getTrace(BaseAction.class);

    @NotNull protected final SynchronizationContext.Complete<F> syncCtx;
    @NotNull protected final ResourceObjectShadowChangeDescription change;
    @NotNull final SynchronizationActionDefinition actionDefinition;
    @NotNull protected final ModelBeans beans = ModelBeans.get();

    BaseAction(@NotNull ActionInstantiationContext<F> ctx) {
        this.syncCtx = ctx.syncCtx;
        this.change = ctx.change;
        this.actionDefinition = ctx.actionDefinition;
    }
}
