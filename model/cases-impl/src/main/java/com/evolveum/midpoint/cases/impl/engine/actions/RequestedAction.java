/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.cases.impl.engine.actions;

import com.evolveum.midpoint.cases.api.request.Request;
import com.evolveum.midpoint.cases.impl.engine.CaseEngineOperationImpl;

import com.evolveum.midpoint.util.logging.Trace;

import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemEventCauseInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemEventCauseTypeType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Action that is invoked on the basis of an external {@link Request}.
 *
 * @see ActionFactory
 */
public abstract class RequestedAction<R extends Request> extends Action {

    @NotNull public final R request;

    RequestedAction(@NotNull CaseEngineOperationImpl ctx, @NotNull R request, @NotNull Trace logger) {
        super(ctx, logger);
        this.request = request;
    }

    @Nullable WorkItemEventCauseTypeType getCauseType() {
        WorkItemEventCauseInformationType causeInformation = getCauseInformation();
        return causeInformation != null ? causeInformation.getType() : null;
    }

    @Nullable WorkItemEventCauseInformationType getCauseInformation() {
        return request.getCauseInformation();
    }
}
