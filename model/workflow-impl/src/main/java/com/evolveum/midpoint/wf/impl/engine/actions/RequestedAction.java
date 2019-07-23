/*
 * Copyright (c) 2010-2019 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
