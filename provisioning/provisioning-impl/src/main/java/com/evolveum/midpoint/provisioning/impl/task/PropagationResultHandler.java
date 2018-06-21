/**
 * Copyright (c) 2018 Evolveum
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
package com.evolveum.midpoint.provisioning.impl.task;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.provisioning.impl.ShadowCache;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.repo.common.task.AbstractSearchIterativeResultHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author semancik
 *
 */
public class PropagationResultHandler extends AbstractSearchIterativeResultHandler<ShadowType> {
	
	private static final transient Trace LOGGER = TraceManager.getTrace(PropagationResultHandler.class);
	
	private final ShadowCache shadowCache;
	private final PrismObject<ResourceType> resource;

	public PropagationResultHandler(Task coordinatorTask, String taskOperationPrefix, TaskManager taskManager, ShadowCache shadowCache, PrismObject<ResourceType> resource) {
		super(coordinatorTask, taskOperationPrefix, "propagation", "to "+resource, taskManager);
		this.shadowCache = shadowCache;
		this.resource = resource;
	}

	protected PrismObject<ResourceType> getResource() {
		return resource;
	}

	@Override
	protected boolean handleObject(PrismObject<ShadowType> shadow, Task workerTask, OperationResult result)
			throws CommonException, PreconditionViolationException {
		try {
			shadowCache.propagateOperations(resource, shadow, workerTask, result);
		} catch (GenericFrameworkException e) {
			throw new SystemException("Generic provisioning framework error: " + e.getMessage(), e);
		}
		return true;
	}

}
