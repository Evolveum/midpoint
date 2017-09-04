/**
 * Copyright (c) 2013-2016 Evolveum
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
package com.evolveum.midpoint.model.impl.sync.action;

import java.util.Map;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.sync.Action;
import com.evolveum.midpoint.model.impl.sync.SynchronizationSituation;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

/**
 * @author semancik
 *
 */
public class AddFocusAction implements Action {

	private static final Trace LOGGER = TraceManager.getTrace(AddFocusAction.class);

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.model.sync.Action#handle(com.evolveum.midpoint.model.lens.LensContext, com.evolveum.midpoint.model.sync.SynchronizationSituation, java.util.Map, com.evolveum.midpoint.task.api.Task, com.evolveum.midpoint.schema.result.OperationResult)
	 */
	@Override
	public <F extends FocusType> void handle(LensContext<F> context, SynchronizationSituation<F> situation,
			Map<QName, Object> parameters, Task task, OperationResult parentResult) throws SchemaException {

		if (context == null) {
			throw new UnsupportedOperationException("addFocus action is not supported with synchronize=false");
		}

		PrismContext prismContext = context.getPrismContext();

		LensFocusContext<F> focusContext = context.createFocusContext();
		Class<F> focusClass = focusContext.getObjectTypeClass();
		LOGGER.trace("addFocus action: add delta for {}", focusClass);
		PrismObjectDefinition<F> focusDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(focusClass);
		PrismObject<F> emptyFocus = focusDefinition.instantiate();
		ObjectDelta<F> delta = emptyFocus.createAddDelta();
        delta.setObjectToAdd(emptyFocus);
        focusContext.setPrimaryDelta(delta);
	}

}
