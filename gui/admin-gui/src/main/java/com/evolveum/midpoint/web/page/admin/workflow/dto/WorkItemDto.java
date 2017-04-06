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

package com.evolveum.midpoint.web.page.admin.workflow.dto;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.visualizer.Scene;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.ObjectTreeDeltas;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.WfContextUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.web.component.DateLabelComponent;
import com.evolveum.midpoint.web.component.prism.show.SceneDto;
import com.evolveum.midpoint.web.component.prism.show.SceneUtil;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskChangesDto;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.apache.commons.collections.CollectionUtils.addIgnoreNull;

/**
 * @author lazyman
 * @author mederly
 */
public class WorkItemDto extends Selectable {

    public static final String F_WORK_ITEM = "workItem";
    public static final String F_NAME = "name";
    public static final String F_CREATED_FORMATTED = "createdFormatted";
    public static final String F_CREATED_FORMATTED_FULL = "createdFormattedFull";
	public static final String F_DEADLINE_FORMATTED = "deadlineFormatted";
    public static final String F_DEADLINE_FORMATTED_FULL = "deadlineFormattedFull";
    public static final String F_STARTED_FORMATTED_FULL = "startedFormattedFull";
    public static final String F_ASSIGNEE_OR_CANDIDATES = "assigneeOrCandidates";
    public static final String F_ORIGINAL_ASSIGNEE = "originalAssignee";
    public static final String F_ORIGINAL_ASSIGNEE_FULL = "originalAssigneeFull";
    public static final String F_CURRENT_ASSIGNEES = "currentAssignees";
    public static final String F_CURRENT_ASSIGNEES_FULL = "currentAssigneesFull";
    public static final String F_CANDIDATES = "candidates";
    public static final String F_STAGE_INFO = "stageInfo";
    public static final String F_ESCALATION_LEVEL_INFO = "escalationLevelInfo";
	public static final String F_ESCALATION_LEVEL_NUMBER = "escalationLevelNumber";
	public static final String F_ADDITIONAL_INFORMATION = "additionalInformation";

	public static final String F_OTHER_WORK_ITEMS = "otherWorkItems";
	public static final String F_RELATED_WORKFLOW_REQUESTS = "relatedWorkflowRequests";

    public static final String F_OBJECT_NAME = "objectName";
    public static final String F_TARGET_NAME = "targetName";

    public static final String F_REQUESTER_NAME = "requesterName";
    public static final String F_REQUESTER_FULL_NAME = "requesterFullName";
    public static final String F_APPROVER_COMMENT = "approverComment";

    public static final String F_WORKFLOW_CONTEXT = "workflowContext";          // use with care
	@Deprecated public static final String F_DELTAS = "deltas";
	public static final String F_PROCESS_INSTANCE_ID = "processInstanceId";
	public static final String F_CHANGES = "changes";

	public static final String F_REQUESTER_COMMENT = "requesterComment";

	// workItem may or may not contain resolved taskRef;
    // and this task may or may not contain filled-in workflowContext -> and then requesterRef object
    //
    // Depending on expected use (work item list vs. work item details)

    private WorkItemType workItem;
	private TaskType taskType;
	private List<TaskType> relatedTasks;
	@Deprecated private SceneDto deltas;
	private TaskChangesDto changes;
	private String approverComment;

    private ObjectType focus;

    public WorkItemDto(WorkItemType workItem) {
        this(workItem, null, null);
    }

	public WorkItemDto(WorkItemType workItem, TaskType taskType, List<TaskType> relatedTasks) {
		this.workItem = workItem;
		this.taskType = taskType;
		this.relatedTasks = relatedTasks;
	}

	public void prepareDeltaVisualization(String sceneNameKey, PrismContext prismContext,
			ModelInteractionService modelInteractionService, Task opTask, OperationResult result) throws SchemaException {
		TaskType task = getTaskType();
		if (task == null || task.getWorkflowContext() == null) {
			return;
		}
		if (!(task.getWorkflowContext().getProcessorSpecificState() instanceof WfPrimaryChangeProcessorStateType)) {
			return;
		}
		WfPrimaryChangeProcessorStateType state = (WfPrimaryChangeProcessorStateType) task.getWorkflowContext().getProcessorSpecificState();
		Scene deltasScene = SceneUtil.visualizeObjectTreeDeltas(state.getDeltasToProcess(), sceneNameKey, prismContext, modelInteractionService, opTask, result);
		deltas = new SceneDto(deltasScene);

		ObjectTreeDeltas deltas = ObjectTreeDeltas.fromObjectTreeDeltasType(state.getDeltasToProcess(), prismContext);
		changes = TaskDto.createChangesToBeApproved(deltas, modelInteractionService,
				prismContext, opTask, result);
	}

	@Nullable
	private TaskType getTaskType() {
    	if (taskType == null) {
			taskType = WebComponentUtil.getObjectFromReference(workItem.getTaskRef(), TaskType.class);
		}
		return taskType;
	}

	public String getWorkItemId() {
        return workItem.getWorkItemId();
    }

    public String getName() {
        return workItem.getName();
    }

    public String getCreatedFormatted() {
        return WebComponentUtil.getLocalizedDate(workItem.getCreateTimestamp(), DateLabelComponent.MEDIUM_MEDIUM_STYLE);
    }

    public String getDeadlineFormatted() {
        return WebComponentUtil.getLocalizedDate(workItem.getDeadline(), DateLabelComponent.MEDIUM_MEDIUM_STYLE);
    }

	public String getCreatedFormattedFull() {
		return WebComponentUtil.getLocalizedDate(workItem.getCreateTimestamp(), DateLabelComponent.FULL_MEDIUM_STYLE);
	}

	public String getDeadlineFormattedFull() {
		return WebComponentUtil.getLocalizedDate(workItem.getDeadline(), DateLabelComponent.FULL_MEDIUM_STYLE);
	}

	public Date getCreatedDate() {
        return XmlTypeConverter.toDate(workItem.getCreateTimestamp());
    }

	public Date getDeadlineDate() {
        return XmlTypeConverter.toDate(workItem.getDeadline());
    }

    public Date getStartedDate() {
        return XmlTypeConverter.toDate(workItem.getProcessStartedTimestamp());
    }

    public String getStartedFormattedFull() {
        return WebComponentUtil.getLocalizedDate(workItem.getProcessStartedTimestamp(), DateLabelComponent.FULL_MEDIUM_STYLE);
    }

    // TODO
    public String getAssigneeOrCandidates() {
        String assignee = getCurrentAssignees();
        if (assignee != null) {
            return assignee;
        } else {
            return getCandidates();
        }
    }

    public String getOriginalAssignee() {
        return WebComponentUtil.getReferencedObjectNames(Collections.singletonList(workItem.getOriginalAssigneeRef()), false);
    }

    public String getOriginalAssigneeFull() {
        return WebComponentUtil.getReferencedObjectDisplayNamesAndNames(Collections.singletonList(workItem.getOriginalAssigneeRef()), false);
    }

    public String getCurrentAssignees() {
        return WebComponentUtil.getReferencedObjectNames(workItem.getAssigneeRef(), false);
    }

    public String getCurrentAssigneesFull() {
        return WebComponentUtil.getReferencedObjectDisplayNamesAndNames(workItem.getAssigneeRef(), false);
    }

    public String getCandidates() {
    	return WebComponentUtil.getReferencedObjectNames(workItem.getCandidateRef(), true);
	}

	public String getObjectName() {
        return WebComponentUtil.getName(workItem.getObjectRef());
    }

	public ObjectReferenceType getObjectRef() {
		return workItem.getObjectRef();
	}

	public ObjectReferenceType getTargetRef() {
		return workItem.getTargetRef();
	}

	public String getTargetName() {
        return WebComponentUtil.getName(workItem.getTargetRef());
    }

    public WfContextType getWorkflowContext() {
        TaskType task = getTaskType();
        return task != null ? task.getWorkflowContext() : null;
    }

    public String getRequesterName() {
		WfContextType workflowContext = getWorkflowContext();
		return workflowContext != null ? WebComponentUtil.getName(workflowContext.getRequesterRef()) : null;
    }

	public String getRequesterFullName() {
		UserType requester = getRequester();
		return requester != null ? PolyString.getOrig(requester.getFullName()) : null;
	}

	public UserType getRequester() {
        WfContextType wfContext = getWorkflowContext();
        if (wfContext == null) {
            return null;
        }
        return WebComponentUtil.getObjectFromReference(wfContext.getRequesterRef(), UserType.class);
    }

    public String getApproverComment() {
        return approverComment;
    }

    public void setApproverComment(String approverComment) {
        this.approverComment = approverComment;
    }

    public WorkItemType getWorkItem() {
        return workItem;
    }

	@Deprecated
	public SceneDto getDeltas() {
		return deltas;
	}

	public QName getTargetType() {
		return workItem.getTargetRef() != null ? workItem.getTargetRef().getType() : null;
	}

	public QName getObjectType() {
		return workItem.getObjectRef() != null ? workItem.getObjectRef().getType() : null;
	}

	// all except the current one
	public List<WorkItemDto> getOtherWorkItems() {
		final List<WorkItemDto> rv = new ArrayList<>();
		final TaskType task = getTaskType();
		if (task == null || task.getWorkflowContext() == null) {
			return rv;
		}
		for (WorkItemType workItemType : task.getWorkflowContext().getWorkItem()) {
			if (workItemType.getWorkItemId() == null || workItemType.getWorkItemId().equals(getWorkItemId())) {
				continue;
			}
			rv.add(new WorkItemDto(workItemType));
		}
		return rv;
	}

	// all excluding the current task
	public List<ProcessInstanceDto> getRelatedWorkflowRequests() {
		final List<ProcessInstanceDto> rv = new ArrayList<>();
		if (relatedTasks == null) {
			return rv;
		}
		for (TaskType task : relatedTasks) {
			if (task.getWorkflowContext() == null || task.getWorkflowContext().getProcessInstanceId() == null) {
				continue;
			}
			if (StringUtils.equals(getProcessInstanceId(), task.getWorkflowContext().getProcessInstanceId())) {
				continue;
			}
			rv.add(new ProcessInstanceDto(task));
		}
		return rv;
	}

	public String getProcessInstanceId() {
		final TaskType task = getTaskType();
		return task != null && task.getWorkflowContext() != null ? task.getWorkflowContext().getProcessInstanceId() : null;
	}

	public String getTaskOid() {
		final TaskType task = getTaskType();
		return task != null ? task.getOid() : null;
	}

	// TODO deduplicate
	public boolean hasHistory() {
		List<DecisionDto> rv = new ArrayList<>();
		WfContextType wfContextType = getWorkflowContext();
		if (wfContextType == null) {
			return false;
		}
		if (!wfContextType.getEvent().isEmpty()) {
			wfContextType.getEvent().forEach(e -> addIgnoreNull(rv, DecisionDto.create(e, null)));
		} else {
			ItemApprovalProcessStateType instanceState = WfContextUtil.getItemApprovalProcessInfo(wfContextType);
			if (instanceState != null) {
				instanceState.getDecisions().forEach(d -> addIgnoreNull(rv, DecisionDto.create(d)));
			}
		}
		return !rv.isEmpty();
	}

	public String getStageInfo() {
    	WfContextType wfc = getWorkflowContext();		// wfc contains also the approval schema
		return wfc != null ? WfContextUtil.getStageInfo(wfc) : WfContextUtil.getStageInfo(workItem);
	}

	public String getEscalationLevelInfo() {
    	return WfContextUtil.getEscalationLevelInfo(workItem);
	}

	public Integer getEscalationLevelNumber() {
    	return workItem.getEscalationLevelNumber() == null || workItem.getEscalationLevelNumber() == 0 ?
				null : workItem.getEscalationLevelNumber();
	}

	public List<InformationType> getAdditionalInformation() {
		return workItem.getAdditionalInformation();
	}

	// Expects that we deal with primary changes of the focus (i.e. not of projections)
	public ObjectType getFocus(PageBase pageBase) {
    	if (focus != null) {
    		return focus;
		}
		WfContextType wfc = getWorkflowContext();
		WfPrimaryChangeProcessorStateType state = WfContextUtil.getPrimaryChangeProcessorState(wfc);
		if (state == null || state.getDeltasToProcess() == null || state.getDeltasToProcess().getFocusPrimaryDelta() == null) {
			return null;
		}
		ObjectDeltaType delta = state.getDeltasToProcess().getFocusPrimaryDelta();
		if (delta.getChangeType() == ChangeTypeType.ADD) {
			focus = (ObjectType) delta.getObjectToAdd();
		} else if (delta.getChangeType() != ChangeTypeType.MODIFY) {
		} else {
			String oid = delta.getOid();
			if (oid == null) {
				throw new IllegalStateException("No OID in object modify delta: " + delta);
			}
			if (delta.getObjectType() == null) {
				throw new IllegalStateException("No object type in object modify delta: " + delta);
			}
			Class<? extends ObjectType> clazz = ObjectTypes.getObjectTypeFromTypeQName(delta.getObjectType())
					.getClassDefinition();
			Task task = pageBase.createSimpleTask("getObject");
			PrismObject<?> object = WebModelServiceUtils.loadObject(clazz, oid, pageBase, task, task.getResult());
			if (object != null) {
				focus = (ObjectType) object.asObjectable();
				try {
					ObjectDelta<Objectable> objectDelta = DeltaConvertor.createObjectDelta(delta, pageBase.getPrismContext());
					objectDelta.applyTo((PrismObject) focus.asPrismObject());
				} catch (SchemaException e) {
					throw new SystemException("Cannot apply delta to focus object: " + e.getMessage(), e);
				}
				focus = (ObjectType) object.asObjectable();
			}
		}
		return focus;
	}

	public String getRequesterComment() {
		OperationBusinessContextType businessContext = WfContextUtil.getBusinessContext(getWorkflowContext());
		return businessContext != null ? businessContext.getComment() : null;
	}
}
