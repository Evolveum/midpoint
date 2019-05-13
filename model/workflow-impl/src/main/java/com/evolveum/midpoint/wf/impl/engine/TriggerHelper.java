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

package com.evolveum.midpoint.wf.impl.engine;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.WfContextUtil;
import com.evolveum.midpoint.schema.util.WorkItemId;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processes.common.WfTimedActionTriggerHandler;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TriggerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemTimedActionsType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 *
 */
@Component
public class TriggerHelper {

	private static final Trace LOGGER = TraceManager.getTrace(TriggerHelper.class);

	@Autowired private RepositoryService repositoryService;
	@Autowired private PrismContext prismContext;

	public void createTriggersForTimedActions(WorkItemId workItemId, int escalationLevel, Date workItemCreateTime,
			Date workItemDeadline, CaseType wfCase, List<WorkItemTimedActionsType> timedActionsList, OperationResult result) {
		LOGGER.trace("Creating triggers for timed actions for work item {}, escalation level {}, create time {}, deadline {}, {} timed action(s)",
				workItemId, escalationLevel, workItemCreateTime, workItemDeadline, timedActionsList.size());
		try {
			List<TriggerType> triggers = WfContextUtil.createTriggers(escalationLevel, workItemCreateTime, workItemDeadline,
					timedActionsList, prismContext, LOGGER, workItemId.asString(), WfTimedActionTriggerHandler.HANDLER_URI);
			LOGGER.trace("Adding {} triggers to {}:\n{}", triggers.size(), wfCase,
					PrismUtil.serializeQuietlyLazily(prismContext, triggers));
			if (!triggers.isEmpty()) {
				List<ItemDelta<?, ?>> itemDeltas = prismContext.deltaFor(TaskType.class)
						.item(TaskType.F_TRIGGER).add(PrismContainerValue.toPcvList(triggers))
						.asItemDeltas();
				repositoryService.modifyObject(CaseType.class, wfCase.getOid(), itemDeltas, result);
			}
		} catch (ObjectNotFoundException | SchemaException | ObjectAlreadyExistsException | RuntimeException e) {
			throw new SystemException("Couldn't add trigger(s) to " + wfCase + ": " + e.getMessage(), e);
		}
	}

	public void removeTriggersForWorkItem(CaseType aCase, WorkItemId workItemId, OperationResult result) {
		List<PrismContainerValue<TriggerType>> toDelete = new ArrayList<>();
		for (TriggerType triggerType : aCase.getTrigger()) {
			if (WfTimedActionTriggerHandler.HANDLER_URI.equals(triggerType.getHandlerUri())) {
				PrismProperty workItemIdProperty = triggerType.getExtension().asPrismContainerValue()
						.findProperty(SchemaConstants.MODEL_EXTENSION_WORK_ITEM_ID);
				if (workItemIdProperty != null && workItemId.asString().equals(workItemIdProperty.getRealValue())) {
					//noinspection unchecked
					toDelete.add(triggerType.clone().asPrismContainerValue());
				}
			}
		}
		removeSelectedTriggers(aCase, toDelete, result);
	}

	// not necessary any more, as work item triggers are deleted when the work item (task) is deleted
	// (and there are currently no triggers other than work-item-related)
	public void removeAllStageTriggersForWorkItem(CaseType aCase, OperationResult result) {
		List<PrismContainerValue<TriggerType>> toDelete = new ArrayList<>();
		for (TriggerType triggerType : aCase.getTrigger()) {
			if (WfTimedActionTriggerHandler.HANDLER_URI.equals(triggerType.getHandlerUri())) {
				//noinspection unchecked
				toDelete.add(triggerType.clone().asPrismContainerValue());
			}
		}
		removeSelectedTriggers(aCase, toDelete, result);
	}

	private void removeSelectedTriggers(CaseType aCase, List<PrismContainerValue<TriggerType>> toDelete, OperationResult result) {
		LOGGER.trace("About to delete {} triggers from {}: {}", toDelete.size(), aCase, toDelete);
		if (!toDelete.isEmpty()) {
			try {
				List<ItemDelta<?, ?>> itemDeltas = prismContext.deltaFor(TaskType.class)
						.item(TaskType.F_TRIGGER).delete(toDelete)
						.asItemDeltas();
				repositoryService.modifyObject(CaseType.class, aCase.getOid(), itemDeltas, result);
			} catch (SchemaException|ObjectNotFoundException|ObjectAlreadyExistsException|RuntimeException e) {
				LoggingUtils.logUnexpectedException(LOGGER, "Couldn't remove triggers from {}", e, aCase);
			}
		}
	}
}
