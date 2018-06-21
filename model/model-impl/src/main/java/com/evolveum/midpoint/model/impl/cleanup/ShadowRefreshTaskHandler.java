/*
 * Copyright (c) 2017 Evolveum
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
package com.evolveum.midpoint.model.impl.cleanup;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.model.impl.util.AbstractScannerResultHandler;
import com.evolveum.midpoint.model.impl.util.AbstractScannerTaskHandler;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Scanner that looks for pending operations in the shadows and updates the status.
 *
 * @author Radovan Semancik
 */
@Component
public class ShadowRefreshTaskHandler extends AbstractScannerTaskHandler<ShadowType, AbstractScannerResultHandler<ShadowType>> {

	// WARNING! This task handler is efficiently singleton!
	// It is a spring bean and it is supposed to handle all search task instances
	// Therefore it must not have task-specific fields. It can only contain fields specific to
	// all tasks of a specified type

	public static final String HANDLER_URI = ModelPublicConstants.SHADOW_REFRESH_TASK_HANDLER_URI;

	private static final transient Trace LOGGER = TraceManager.getTrace(ShadowRefreshTaskHandler.class);

	@Autowired(required = true)
    protected ProvisioningService provisioningService;

	public ShadowRefreshTaskHandler() {
        super(ShadowType.class, "Shadow refresh", OperationConstants.SHADOW_REFRESH);
    }

	@PostConstruct
	private void initialize() {
		taskManager.registerHandler(HANDLER_URI, this);
	}

	@Override
	protected Class<ShadowType> getType(Task task) {
		return ShadowType.class;
	}

	@Override
	protected boolean useRepositoryDirectly(AbstractScannerResultHandler<ShadowType> resultHandler, TaskRunResult runResult, Task coordinatorTask, OperationResult opResult) {
        return true;
    }

	@Override
	protected ObjectQuery createQuery(AbstractScannerResultHandler<ShadowType> handler, TaskRunResult runResult, Task task, OperationResult opResult) throws SchemaException {

		ObjectQuery query = new ObjectQuery();
		ObjectFilter filter = QueryBuilder.queryFor(ShadowType.class, prismContext)
				.exists(ShadowType.F_PENDING_OPERATION)
			.buildFilter();

		query.setFilter(filter);
		return query;
	}

	@Override
	protected void finish(AbstractScannerResultHandler<ShadowType> handler, TaskRunResult runResult, Task task, OperationResult opResult)
			throws SchemaException {
		super.finish(handler, runResult, task, opResult);
	}

	@Override
	protected AbstractScannerResultHandler<ShadowType> createHandler(TaskRunResult runResult, final Task coordinatorTask,
			OperationResult opResult) {

		AbstractScannerResultHandler<ShadowType> handler = new AbstractScannerResultHandler<ShadowType>(
				coordinatorTask, ShadowRefreshTaskHandler.class.getName(), "shadowRefresh", "shadow refresh task", taskManager) {
			@Override
			protected boolean handleObject(PrismObject<ShadowType> object, Task workerTask, OperationResult result) throws CommonException {
				LOGGER.trace("Refreshing {}", object);

				provisioningService.refreshShadow(object, null, workerTask, result);

				LOGGER.trace("Refreshed {}", object);
				return true;
			}
		};
        handler.setStopOnError(false);
        return handler;
	}

}
