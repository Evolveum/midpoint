/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.engine.actions;

import com.evolveum.midpoint.wf.impl.engine.EngineInvocationContext;
import org.jetbrains.annotations.NotNull;

/**
 *  This is an action that is invoked internally i.e. without a request.
 *  (Currently it's used as a marker class.)
 */
public abstract class InternalAction extends Action {

	InternalAction(@NotNull EngineInvocationContext ctx) {
		super(ctx);
	}
}
