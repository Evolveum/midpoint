/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
