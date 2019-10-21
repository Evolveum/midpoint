/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.engine.actions;

import com.evolveum.midpoint.wf.api.request.Request;
import com.evolveum.midpoint.wf.impl.engine.EngineInvocationContext;
import org.jetbrains.annotations.NotNull;

/**
 *  Action that is invoked on the basis of an external request.
 */
public abstract class RequestedAction<R extends Request> extends Action {

    @NotNull public final R request;

    RequestedAction(@NotNull EngineInvocationContext ctx, @NotNull R request) {
        super(ctx);
        this.request = request;
    }
}
