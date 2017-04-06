/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.wf.impl.processes.common;

import com.evolveum.midpoint.model.api.expr.MidpointFunctions;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.WfContextUtil;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.ProcessVariableNames;
import com.evolveum.midpoint.wf.impl.util.MiscDataUtil;
import com.evolveum.midpoint.wf.impl.util.SerializationSafeContainer;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.activiti.engine.delegate.DelegateExecution;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.wf.impl.processes.common.CommonProcessVariableNames.*;
import static com.evolveum.midpoint.wf.impl.processes.common.SpringApplicationContextHolder.getMidpointFunctions;
import static com.evolveum.midpoint.wf.impl.processes.common.SpringApplicationContextHolder.getTaskManager;

/**
 * General utilities that can be used from within processes.
 *
 * @author mederly
 */
public class ActivitiUtil implements Serializable {

    private static final long serialVersionUID = 5183098710717369392L;

    private static final Trace LOGGER = TraceManager.getTrace(ActivitiUtil.class);

	@NotNull
	public static ApprovalStageDefinitionType getAndVerifyCurrentStage(DelegateExecution execution, Task wfTask, boolean stageInContextSet,
			PrismContext prismContext) {
		int stageNumber = getRequiredVariable(execution, ProcessVariableNames.STAGE_NUMBER_LOCAL, Integer.class, prismContext);
		ApprovalStageDefinitionType stageDefinition;
		if (stageInContextSet) {
			stageDefinition = WfContextUtil.getCurrentStageDefinition(wfTask.getWorkflowContext());
			if (stageDefinition == null) {
				throw new IllegalStateException("No current stage information in " + wfTask);
			}
		} else {
			stageDefinition = WfContextUtil.getStageDefinition(wfTask.getWorkflowContext(), stageNumber);
			if (stageDefinition == null) {
				throw new IllegalStateException("No stage #" + stageNumber + " in " + wfTask);
			}
		}
		return stageDefinition;
	}

	public PrismContext getPrismContext() {
        return SpringApplicationContextHolder.getPrismContext();
    }

    public void revive(SerializationSafeContainer<?> container) {
        container.setPrismContext(SpringApplicationContextHolder.getPrismContext());
    }

    // todo - better name?
    public MidpointFunctions midpoint() {
        return getMidpointFunctions();
    }

    @Override
    public String toString() {
        return this.getClass().getName() + " object.";
    }

    @NotNull
    public static String getTaskOid(Map<String, Object> variables) {
		return ActivitiUtil.getRequiredVariable(variables, CommonProcessVariableNames.VARIABLE_MIDPOINT_TASK_OID, String.class, null);
	}

    @NotNull
    public static Task getTask(DelegateExecution execution, OperationResult result) {
        String oid = getTaskOid(execution.getVariables());
		try {
			return getTaskManager().getTask(oid, result);
		} catch (ObjectNotFoundException|SchemaException|RuntimeException e) {
			throw new SystemException("Couldn't get task " + oid + " corresponding to process " + execution.getProcessInstanceId(), e);
		}
	}

	@SuppressWarnings("unchecked")
    public static <T> T getRequiredVariable(DelegateExecution execution, String name, Class<T> clazz, PrismContext prismContext) {
		Object value = getVariable(execution.getVariables(), name, clazz, prismContext);
		if (value == null) {
			throw new IllegalStateException("Required process variable " + name + " is missing in " + execution);
		} else {
			return (T) value;
		}
	}

    public static <T> T getRequiredVariable(Map<String, Object> variables, String name, Class<T> clazz, PrismContext prismContext) {
        Object value = getVariable(variables, name, clazz, prismContext);
        if (value == null) {
            throw new IllegalStateException("Required process variable " + name + " is missing");
        } else {
            return (T) value;
        }
    }

    public static <T> T getVariable(DelegateExecution execution, String name, Class<T> clazz, PrismContext prismContext) {
    	return getVariable(execution.getVariables(), name, clazz, prismContext);
	}

	@SuppressWarnings("unchecked")
    public static <T> T getVariable(Map<String, Object> variables, String name, Class<T> clazz, PrismContext prismContext) {
		Object value = variables.get(name);
		if (value instanceof SerializationSafeContainer && !SerializationSafeContainer.class.isAssignableFrom(clazz)) {
			SerializationSafeContainer container = (SerializationSafeContainer) value;
			if (container.getPrismContext() == null && prismContext != null) {
				container.setPrismContext(prismContext);
			}
			value = container.getValue();
		}
		if (value != null && !(clazz.isAssignableFrom(value.getClass()))) {
			throw new IllegalStateException("Process variable " + name + " should be of " + clazz + " but is of "
					+ value.getClass() + " instead.");
		} else {
			return (T) value;
		}
	}

    public static <T> T getVariable(Map<String, Object> variables, String name, Class<T> clazz) {
        return getVariable(variables, name, clazz, null);
    }

    @NotNull
	public static WfContextType getWorkflowContext(Task wfTask) {
		if (wfTask == null) {
			throw new IllegalArgumentException("No task");
		} else if (wfTask.getWorkflowContext() == null) {
			throw new IllegalArgumentException("No workflow context in task " + wfTask);
		} else {
			return wfTask.getWorkflowContext();
		}
	}

	public static List<LightweightObjectRef> toLightweightReferences(Collection<ObjectReferenceType> refs) {
		return refs.stream().map(ort -> new LightweightObjectRefImpl(ort)).collect(Collectors.toList());
	}

	// TODO move to better place (it is called also from WorkItemManager)
	// Make sure this does not refer to variables modified in activity db but not in the Java task object
	public static void fillInWorkItemEvent(WorkItemEventType event, MidPointPrincipal currentUser, String workItemId,
			Map<String, Object> variables, PrismContext prismContext) {
		if (currentUser != null) {
			event.setInitiatorRef(ObjectTypeUtil.createObjectRef(currentUser.getUser()));
		}
		event.setTimestamp(XmlTypeConverter.createXMLGregorianCalendar(new Date()));
		event.setExternalWorkItemId(workItemId);
		String originalAssigneeString = ActivitiUtil.getVariable(variables, VARIABLE_ORIGINAL_ASSIGNEE, String.class, prismContext);
		if (originalAssigneeString != null) {
			event.setOriginalAssigneeRef(MiscDataUtil.stringToRef(originalAssigneeString));
		}
		event.setStageNumber(ActivitiUtil.getRequiredVariable(variables, VARIABLE_STAGE_NUMBER, Integer.class, prismContext));
		//event.setStageName(ActivitiUtil.getVariable(variables, VARIABLE_STAGE_NAME, String.class, prismContext));
		//event.setStageDisplayName(ActivitiUtil.getVariable(variables, VARIABLE_STAGE_DISPLAY_NAME, String.class, prismContext));
		event.setEscalationLevel(WfContextUtil.createEscalationLevel(ActivitiUtil.getEscalationLevelNumber(variables),
				ActivitiUtil.getVariable(variables, VARIABLE_ESCALATION_LEVEL_NAME, String.class, prismContext),
				ActivitiUtil.getVariable(variables, VARIABLE_ESCALATION_LEVEL_DISPLAY_NAME, String.class, prismContext)));
	}

	public static int getEscalationLevelNumber(Map<String, Object> variables) {
		Integer e = ActivitiUtil.getVariable(variables, VARIABLE_ESCALATION_LEVEL_NUMBER, Integer.class, null);
		return e != null ? e : 0;
	}
}
