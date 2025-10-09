/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.cases.impl.engine.actions;

import com.evolveum.midpoint.cases.impl.engine.CaseEngineOperationImpl;

import com.evolveum.midpoint.util.logging.Trace;

import org.jetbrains.annotations.NotNull;

/**
 *  This is an action that is invoked internally i.e. without a request.
 *  (Currently it's used as a marker class.)
 */
public abstract class InternalAction extends Action {

    InternalAction(@NotNull CaseEngineOperationImpl ctx, @NotNull Trace logger) {
        super(ctx, logger);
    }
}
