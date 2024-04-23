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

import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizeSynchronizationActionType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

/**
 * @author semancik
 *
 */
@ActionUris({
        "http://midpoint.evolveum.com/xml/ns/public/model/action-3#modifyUser",
        "http://midpoint.evolveum.com/xml/ns/public/model/action-3#synchronize" })
@ActionDefinitionClass(SynchronizeSynchronizationActionType.class)
class SynchronizeAction<F extends FocusType> extends BaseClockworkAction<F> {

    SynchronizeAction(@NotNull ActionInstantiationContext<F> ctx) {
        super(ctx);
    }

    @Override
    void prepareContext(@NotNull LensContext<F> context, @NotNull OperationResult result) {
        // Nothing to do here
    }
}
