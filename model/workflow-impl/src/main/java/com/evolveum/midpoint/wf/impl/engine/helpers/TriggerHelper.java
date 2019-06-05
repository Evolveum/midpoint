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

package com.evolveum.midpoint.wf.impl.engine.helpers;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.WfContextUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processes.common.WfTimedActionTriggerHandler;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TriggerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemTimedActionsType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 *
 */
@Component
public class TriggerHelper {

	private static final Trace LOGGER = TraceManager.getTrace(TriggerHelper.class);

	@Autowired private PrismContext prismContext;

	public void createTriggersForTimedActions(CaseType currentCase, long workItemId, int escalationLevel,
			Date workItemCreateTime,
			Date workItemDeadline, List<WorkItemTimedActionsType> timedActionsList, OperationResult result) {
		LOGGER.trace("Creating triggers for timed actions for work item {}, escalation level {}, create time {}, deadline {}, {} timed action(s)",
				workItemId, escalationLevel, workItemCreateTime, workItemDeadline, timedActionsList.size());
		try {
			List<TriggerType> triggers = WfContextUtil.createTriggers(escalationLevel, workItemCreateTime, workItemDeadline,
					timedActionsList, prismContext, LOGGER, workItemId, WfTimedActionTriggerHandler.HANDLER_URI);
			LOGGER.trace("Adding {} triggers to {}:\n{}", triggers.size(), currentCase,
					PrismUtil.serializeQuietlyLazily(prismContext, triggers));
			currentCase.getTrigger().addAll(triggers);
		} catch (SchemaException | RuntimeException e) {
			throw new SystemException("Couldn't add trigger(s) to " + currentCase + ": " + e.getMessage(), e);
		}
	}

	void removeTriggersForWorkItem(CaseType aCase, long workItemId, OperationResult result) {
		for (Iterator<TriggerType> iterator = aCase.getTrigger().iterator(); iterator.hasNext(); ) {
			TriggerType triggerType = iterator.next();
			if (WfTimedActionTriggerHandler.HANDLER_URI.equals(triggerType.getHandlerUri())) {
				//noinspection unchecked
				PrismProperty<Long> workItemIdProperty = triggerType.getExtension().asPrismContainerValue()
						.findProperty(SchemaConstants.MODEL_EXTENSION_WORK_ITEM_ID);
				if (workItemIdProperty != null) {
					Long realValue = workItemIdProperty.getRealValue();
					if (realValue != null && realValue == workItemId) {
						iterator.remove();
					}
				}
			}
		}
	}

	// not necessary any more, as work item triggers are deleted when the work item (task) is deleted
	// (and there are currently no triggers other than work-item-related)
	public void removeAllStageTriggersForWorkItem(CaseType aCase) {
		aCase.getTrigger().removeIf(triggerType -> WfTimedActionTriggerHandler.HANDLER_URI.equals(triggerType.getHandlerUri()));
	}

//	private void removeSelectedTriggers(CaseType aCase, List<PrismContainerValue<TriggerType>> toDelete, OperationResult result) {
//		LOGGER.trace("About to delete {} triggers from {}: {}", toDelete.size(), aCase, toDelete);
//		if (!toDelete.isEmpty()) {
//			try {
//				List<ItemDelta<?, ?>> itemDeltas = prismContext.deltaFor(TaskType.class)
//						.item(TaskType.F_TRIGGER).delete(toDelete)
//						.asItemDeltas();
//				repositoryService.modifyObject(CaseType.class, aCase.getOid(), itemDeltas, result);
//			} catch (SchemaException|ObjectNotFoundException|ObjectAlreadyExistsException|RuntimeException e) {
//				LoggingUtils.logUnexpectedException(LOGGER, "Couldn't remove triggers from {}", e, aCase);
//			}
//		}
//	}
}
