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

package com.evolveum.midpoint.wf.impl.execution;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CaseTypeUtil;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.access.AuthorizationHelper;
import com.evolveum.midpoint.wf.impl.engine.helpers.AuditHelper;
import com.evolveum.midpoint.wf.impl.engine.helpers.NotificationHelper;
import com.evolveum.midpoint.wf.impl.engine.helpers.TriggerHelper;
import com.evolveum.midpoint.wf.impl.engine.helpers.WorkItemHelper;
import com.evolveum.midpoint.wf.impl.processes.common.ExpressionEvaluationHelper;
import com.evolveum.midpoint.wf.impl.processes.common.StageComputeHelper;
import com.evolveum.midpoint.wf.impl.processors.primary.PrimaryChangeProcessor;
import com.evolveum.midpoint.wf.impl.util.MiscHelper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 */
@Component
public class ExecutionHelper {

	private static final Trace LOGGER = TraceManager.getTrace(ExecutionHelper.class);

	@Autowired public Clock clock;
	@Autowired
	@Qualifier("cacheRepositoryService")
	public RepositoryService repositoryService;
	@Autowired public PrismContext prismContext;
	@Autowired private TaskManager taskManager;
	@Autowired public AuditHelper auditHelper;
	@Autowired public NotificationHelper notificationHelper;
	@Autowired public StageComputeHelper stageComputeHelper;
	@Autowired public PrimaryChangeProcessor primaryChangeProcessor;   // todo
	@Autowired public MiscHelper miscHelper;
	@Autowired public TriggerHelper triggerHelper;
	@Autowired public ExpressionEvaluationHelper expressionEvaluationHelper;
	@Autowired public WorkItemHelper workItemHelper;
	@Autowired public AuthorizationHelper authorizationHelper;

	public void closeCaseInRepository(CaseType aCase, OperationResult result)
			throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
		List<ItemDelta<?, ?>> modifications = prismContext.deltaFor(CaseType.class)
				.item(CaseType.F_STATE).replace(SchemaConstants.CASE_STATE_CLOSED)
				.item(CaseType.F_CLOSE_TIMESTAMP).replace(clock.currentTimeXMLGregorianCalendar())
				.asItemDeltas();
		repositoryService.modifyObject(CaseType.class, aCase.getOid(), modifications, result);
		LOGGER.debug("Marked case {} as closed", aCase);
	}

	public void setCaseStateInRepository(CaseType aCase, String newState, OperationResult result)
			throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
		List<ItemDelta<?, ?>> modifications = prismContext.deltaFor(CaseType.class)
				.item(CaseType.F_STATE).replace(newState)
				.asItemDeltas();
		repositoryService.modifyObject(CaseType.class, aCase.getOid(), modifications, result);
		LOGGER.debug("Marked case {} as {}", aCase, newState);
	}

	/**
	 * We need to check
	 * 1) if there are any executable cases that depend on this one
	 * 2) if we can close the parent (root)
	 */
	public void checkDependentCases(String rootOid, OperationResult result)
			throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException, PreconditionViolationException {
		CaseType rootCase = repositoryService.getObject(CaseType.class, rootOid, null, result).asObjectable();
		if (CaseTypeUtil.isClosed(rootCase)) {
			return;
		}
		List<CaseType> subcases = miscHelper.getSubcases(rootOid, result);
		LOGGER.debug("Subcases:");
		for (CaseType subcase : subcases) {
			LOGGER.debug(" - {}: state={}, closeTS={}", subcase, subcase.getState(), subcase.getCloseTimestamp());
		}
		List<String> openOids = subcases.stream()
				.filter(c -> !CaseTypeUtil.isClosed(c))
				.map(ObjectType::getOid)
				.collect(Collectors.toList());
		LOGGER.debug("open cases OIDs: {}", openOids);
		if (openOids.isEmpty()) {
			closeCaseInRepository(rootCase, result);
		} else {
			ObjectQuery query = prismContext.queryFor(TaskType.class)
					.item(TaskType.F_OBJECT_REF).ref(openOids.toArray(new String[0]))
					.and().item(TaskType.F_EXECUTION_STATUS).eq(TaskExecutionStatusType.WAITING)
					.build();
			SearchResultList<PrismObject<TaskType>> waitingTasks = repositoryService
					.searchObjects(TaskType.class, query, null, result);
			LOGGER.debug("Waiting tasks: {}", waitingTasks);
			for (PrismObject<TaskType> waitingTask : waitingTasks) {
				String waitingCaseOid = waitingTask.asObjectable().getObjectRef().getOid();
				assert waitingCaseOid != null;
				List<CaseType> waitingCaseList = subcases.stream().filter(c -> waitingCaseOid.equals(c.getOid()))
						.collect(Collectors.toList());
				assert waitingCaseList.size() == 1;
				Set<String> prerequisiteOids = waitingCaseList.get(0).getPrerequisiteRef().stream()
						.map(ObjectReferenceType::getOid)
						.collect(Collectors.toSet());
				Collection<String> openPrerequisites = CollectionUtils.intersection(prerequisiteOids, openOids);
				LOGGER.trace("prerequisite OIDs = {}; intersection with open OIDs = {}", prerequisiteOids, openPrerequisites);
				if (openPrerequisites.isEmpty()) {
					LOGGER.trace("All prerequisites are fulfilled, going to release the task {}", waitingTask);
					taskManager.unpauseTask(taskManager.createTaskInstance(waitingTask, result), result);
				} else {
					LOGGER.trace("...task is not released and continues waiting for those cases");
				}
			}
		}
	}
}
