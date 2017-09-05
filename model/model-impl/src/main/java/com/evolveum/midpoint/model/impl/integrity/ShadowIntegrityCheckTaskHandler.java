/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.model.impl.integrity;

import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.model.common.SystemObjectCache;
import com.evolveum.midpoint.model.impl.sync.SynchronizationService;
import com.evolveum.midpoint.model.impl.util.AbstractSearchIterativeTaskHandler;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * Task handler for "Shadow integrity check" task.
 *
 * The purpose of this task is to detect and optionally delete duplicate shadows, i.e. distinct shadows that
 * correspond to the same resource object.
 *
 *  * Task handler for "Normalize attribute/property data" task.
 *
 * The purpose of this task is to normalize data stored in repository when the corresponding matching rule changes
 * (presumably from non-normalizing to normalizing one, e.g. from case sensitive to case insensitive).
 *
 * The reason is that if the data in the repository would be stored in non-normalized form, the would be
 * effectively hidden for any search on that particular attribute.

 *
 * @author Pavol Mederly
 */
@Component
public class ShadowIntegrityCheckTaskHandler extends AbstractSearchIterativeTaskHandler<ShadowType, ShadowIntegrityCheckResultHandler> {

    public static final String HANDLER_URI = ModelPublicConstants.SHADOW_INTEGRITY_CHECK_TASK_HANDLER_URI;

    // WARNING! This task handler is efficiently singleton!
 	// It is a spring bean and it is supposed to handle all search task instances
 	// Therefore it must not have task-specific fields. It can only contain fields specific to
 	// all tasks of a specified type

    @Autowired
    private ProvisioningService provisioningService;

    @Autowired
    private MatchingRuleRegistry matchingRuleRegistry;

    @Autowired
    private SynchronizationService synchronizationService;

    @Autowired
	private SystemObjectCache systemObjectCache;

    private static final Trace LOGGER = TraceManager.getTrace(ShadowIntegrityCheckTaskHandler.class);

    public ShadowIntegrityCheckTaskHandler() {
        super("Shadow integrity check", OperationConstants.CHECK_SHADOW_INTEGRITY);
        setLogFinishInfo(true);
        setPreserveStatistics(false);
    }

    @PostConstruct
    private void initialize() {
        taskManager.registerHandler(HANDLER_URI, this);
    }

	@Override
	protected ShadowIntegrityCheckResultHandler createHandler(TaskRunResult runResult, Task coordinatorTask, OperationResult opResult) {
        return new ShadowIntegrityCheckResultHandler(coordinatorTask, ShadowIntegrityCheckTaskHandler.class.getName(),
				"check shadow integrity", "check shadow integrity", taskManager, prismContext, provisioningService,
                matchingRuleRegistry, repositoryService, synchronizationService, systemObjectCache, opResult);
	}

	@Override
	protected boolean initializeRun(ShadowIntegrityCheckResultHandler handler,
			TaskRunResult runResult, Task task, OperationResult opResult) {
		return super.initializeRun(handler, runResult, task, opResult);
	}

    @Override
    protected Class<? extends ObjectType> getType(Task task) {
        return ShadowType.class;
    }

    @Override
	protected ObjectQuery createQuery(ShadowIntegrityCheckResultHandler handler, TaskRunResult runResult, Task task, OperationResult opResult) throws SchemaException {
        ObjectQuery query = createQueryFromTask(handler, runResult, task, opResult);
        LOGGER.info("Using query:\n{}", query.debugDump());
        return query;
	}

    @Override
    protected boolean useRepositoryDirectly(ShadowIntegrityCheckResultHandler resultHandler, TaskRunResult runResult, Task coordinatorTask, OperationResult opResult) {
        return true;
    }

    @Override
    public String getCategoryName(Task task) {
        return TaskCategory.UTIL;
    }

    @Override
    public List<String> getCategoryNames() {
        return null;
    }
}
